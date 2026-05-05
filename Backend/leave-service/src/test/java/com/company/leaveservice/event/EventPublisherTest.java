package com.company.leaveservice.event;

import com.company.leaveservice.dto.UserRegisteredEvent;
import com.company.leaveservice.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.company.leaveservice.config.RabbitMQConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ═══════════════════════════════════════════════════════════════
// EventPublisher Tests
// ═══════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher Unit Tests")
class EventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private EventPublisher eventPublisher;

    private LeaveStatusEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = LeaveStatusEvent.builder()
                .leaveId(1L)
                .userId(10L)
                .userEmail("john@example.com")
                .leaveType("CASUAL")
                .status("APPLIED")
                .startDate("2026-04-01")
                .endDate("2026-04-02")
                .build();
    }

    @Nested
    @DisplayName("publishLeaveApplied()")
    class PublishLeaveApplied {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishLeaveApplied_CorrectExchangeAndKey() {
            eventPublisher.publishLeaveApplied(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    LEAVE_EXCHANGE, LEAVE_APPLIED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Does not throw if RabbitMQ fails — swallows exception")
        void publishLeaveApplied_SwallowsException() {
            doThrow(new RuntimeException("Broker down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // Should NOT propagate the exception
            assertDoesNotThrow(() -> eventPublisher.publishLeaveApplied(sampleEvent));
        }
    }

    @Nested
    @DisplayName("publishLeaveApproved()")
    class PublishLeaveApproved {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishLeaveApproved_CorrectExchangeAndKey() {
            eventPublisher.publishLeaveApproved(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    LEAVE_EXCHANGE, LEAVE_APPROVED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows exception on broker failure")
        void publishLeaveApproved_SwallowsException() {
            doThrow(new RuntimeException("timeout"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishLeaveApproved(sampleEvent));
        }
    }

    @Nested
    @DisplayName("publishLeaveRejected()")
    class PublishLeaveRejected {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishLeaveRejected_CorrectExchangeAndKey() {
            eventPublisher.publishLeaveRejected(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    LEAVE_EXCHANGE,LEAVE_REJECTED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows exception on broker failure")
        void publishLeaveRejected_SwallowsException() {
            doThrow(new RuntimeException("timeout"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishLeaveRejected(sampleEvent));
        }
    }

    @Nested
    @DisplayName("publishLeaveCancelled()")
    class PublishLeaveCancelled {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishLeaveCancelled_CorrectExchangeAndKey() {
            eventPublisher.publishLeaveCancelled(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    LEAVE_EXCHANGE, LEAVE_CANCELLED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows exception on broker failure")
        void publishLeaveCancelled_SwallowsException() {
            doThrow(new RuntimeException("timeout"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishLeaveCancelled(sampleEvent));
        }
    }
}


// ═══════════════════════════════════════════════════════════════
// EventConsumer Tests
// ═══════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumer Unit Tests")
class EventConsumerTest {

    @Mock private LeaveService leaveService;
    @InjectMocks private EventConsumer eventConsumer;

    @Test
    @DisplayName("consumeUserDeletedEvent — delegates to softDeleteUserData")
    void consumeUserDeleted_CallsSoftDelete() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(5L).email("user@example.com").build();

        eventConsumer.consumeUserDeletedEvent(event);

        verify(leaveService).softDeleteUserData(5L);
    }

    @Test
    @DisplayName("consumeUserDeletedEvent — does not propagate exception from service")
    void consumeUserDeleted_SwallowsServiceException() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(5L).email("user@example.com").build();

        doThrow(new RuntimeException("DB error"))
                .when(leaveService).softDeleteUserData(5L);

        // Consumer must catch internally and log — must NOT propagate
        assertDoesNotThrow(() -> eventConsumer.consumeUserDeletedEvent(event));
    }

    @Test
    @DisplayName("consumeUserDeletedEvent — correct userId is passed")
    void consumeUserDeleted_PassesCorrectUserId() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(42L).email("user@example.com").build();

        eventConsumer.consumeUserDeletedEvent(event);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(leaveService).softDeleteUserData(captor.capture());
        assertEquals(42L, captor.getValue());
    }
}


// ═══════════════════════════════════════════════════════════════
// UserEventListener Tests
// ═══════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventListener Unit Tests")
class UserEventListenerTest {

    @Mock private LeaveService leaveService;
    @InjectMocks private UserEventListener userEventListener;

    @Test
    @DisplayName("handleUserRegistered — delegates to initializeLeaveBalances")
    void handleUserRegistered_CallsInitialize() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(10L)
                .email("new@example.com")
                .fullName("New User")
                .employeeCode("EMP001")
                .role("EMPLOYEE")
                .build();

        userEventListener.handleUserRegistered(event);

        verify(leaveService).initializeLeaveBalances(10L);
    }

    @Test
    @DisplayName("handleUserRegistered — does not propagate service exception")
    void handleUserRegistered_SwallowsException() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(10L).email("new@example.com").build();

        doThrow(new RuntimeException("DB failure"))
                .when(leaveService).initializeLeaveBalances(10L);

        assertDoesNotThrow(() -> userEventListener.handleUserRegistered(event));
    }

    @Test
    @DisplayName("handleUserRegistered — correct userId is passed")
    void handleUserRegistered_PassesCorrectUserId() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(99L).email("x@example.com").build();

        userEventListener.handleUserRegistered(event);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(leaveService).initializeLeaveBalances(captor.capture());
        assertEquals(99L, captor.getValue());
    }
}