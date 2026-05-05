package com.company.timesheetservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.company.timesheetservice.config.RabbitMQConfig.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher Unit Tests")
class EventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private EventPublisher eventPublisher;

    private TimesheetStatusEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = TimesheetStatusEvent.builder()
                .timesheetId(1L)
                .userId(10L)
                .userEmail("john@example.com")
                .status("SUBMITTED")
                .weekStart("2026-03-16")
                .build();
    }

    @Nested
    @DisplayName("publishTimesheetSubmitted()")
    class PublishSubmitted {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishSubmitted_CorrectExchangeAndKey() {
            eventPublisher.publishTimesheetSubmitted(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    TIMESHEET_EXCHANGE, TIMESHEET_SUBMITTED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows broker exception — does not propagate")
        void publishSubmitted_SwallowsException() {
            doThrow(new RuntimeException("Broker down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishTimesheetSubmitted(sampleEvent));
        }
    }

    @Nested
    @DisplayName("publishTimesheetApproved()")
    class PublishApproved {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishApproved_CorrectExchangeAndKey() {
            eventPublisher.publishTimesheetApproved(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    TIMESHEET_EXCHANGE, TIMESHEET_APPROVED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows broker exception")
        void publishApproved_SwallowsException() {
            doThrow(new RuntimeException("timeout"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishTimesheetApproved(sampleEvent));
        }
    }

    @Nested
    @DisplayName("publishTimesheetRejected()")
    class PublishRejected {

        @Test
        @DisplayName("Sends to correct exchange and routing key")
        void publishRejected_CorrectExchangeAndKey() {
            eventPublisher.publishTimesheetRejected(sampleEvent);

            verify(rabbitTemplate).convertAndSend(
                    TIMESHEET_EXCHANGE, TIMESHEET_REJECTED_KEY, sampleEvent);
        }

        @Test
        @DisplayName("Swallows broker exception")
        void publishRejected_SwallowsException() {
            doThrow(new RuntimeException("timeout"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> eventPublisher.publishTimesheetRejected(sampleEvent));
        }
    }
}