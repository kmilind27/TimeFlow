package com.company.authservice.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.company.authservice.config.RabbitMQConfig.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    // ── Test Data ─────────────────────────────────────────────

    private UserRegisteredEvent userRegisteredEvent;
    private UserRegisteredEvent roleChangedEvent;
    private UserDeletedEvent userDeletedEvent;
   

    @BeforeEach
    void setUp() {

        // ✅ UserRegisteredEvent fields: userId, email, fullName, employeeCode, role
        userRegisteredEvent = UserRegisteredEvent.builder()
                .userId(1L)
                .email("john@test.com")
                .fullName("John Doe")
                .employeeCode("EMP001")
                .role("EMPLOYEE")
                .build();

        // Role changed event — same class, different role
        roleChangedEvent = UserRegisteredEvent.builder()
                .userId(1L)
                .email("john@test.com")
                .fullName("John Doe")
                .employeeCode("EMP001")
                .role("MANAGER")
                .build();

        // ✅ UserDeletedEvent fields: userId, email
        userDeletedEvent = UserDeletedEvent.builder()
                .userId(1L)
                .email("john@test.com")
                .build();
    }

    
    @Test
    void publishUserRegistered_shouldCallRabbitTemplateWithCorrectArgs() {
        eventPublisher.publishUserRegistered(userRegisteredEvent);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE,
                USER_REGISTERED_KEY,
                userRegisteredEvent
        );
    }

    @Test
    void publishUserRegistered_shouldCallRabbitTemplateOnce() {
        eventPublisher.publishUserRegistered(userRegisteredEvent);

        verify(rabbitTemplate, times(1))
                .convertAndSend(AUTH_EXCHANGE, USER_REGISTERED_KEY, userRegisteredEvent);
    }

    @Test
    void publishUserRegistered_withDifferentUser_shouldSendCorrectEvent() {
        UserRegisteredEvent adminEvent = UserRegisteredEvent.builder()
                .userId(2L)
                .email("admin@test.com")
                .fullName("Admin User")
                .employeeCode("EMP000")
                .role("ADMIN")
                .build();

        eventPublisher.publishUserRegistered(adminEvent);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_REGISTERED_KEY, adminEvent);
    }

    @Test
    void publishUserRegistered_whenRabbitThrows_shouldNotPropagateException() {
        // ✅ Your publisher catches exception and logs — must not throw
        doThrow(new RuntimeException("RabbitMQ connection refused"))
                .when(rabbitTemplate).convertAndSend(
                        AUTH_EXCHANGE, USER_REGISTERED_KEY, userRegisteredEvent);

        // ✅ Should NOT throw — caught in try-catch
        eventPublisher.publishUserRegistered(userRegisteredEvent);

        // Method completed without exception
        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_REGISTERED_KEY, userRegisteredEvent);
    }

    @Test
    void publishUserRegistered_whenRabbitDown_shouldStillComplete() {
        doThrow(new org.springframework.amqp.AmqpConnectException(
                new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(
                        anyString(), anyString(), any(UserRegisteredEvent.class));

        assertThatCode(() ->
                eventPublisher.publishUserRegistered(userRegisteredEvent)
        ).doesNotThrowAnyException();

        verify(rabbitTemplate)
                .convertAndSend(
                        AUTH_EXCHANGE,
                        USER_REGISTERED_KEY,
                        userRegisteredEvent
                );
    }

    // ════════════════════════════════════════════════════════
    // publishUserRoleChanged
    // ✅ Sends to AUTH_EXCHANGE with USER_ROLE_CHANGED_KEY
    // ════════════════════════════════════════════════════════

    @Test
    void publishUserRoleChanged_shouldCallRabbitTemplateWithCorrectArgs() {
        eventPublisher.publishUserRoleChanged(roleChangedEvent);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE,
                USER_ROLE_CHANGED_KEY,
                roleChangedEvent
        );
    }

    @Test
    void publishUserRoleChanged_shouldUseRoleChangedKey_notRegisteredKey() {
        eventPublisher.publishUserRoleChanged(roleChangedEvent);

        // ✅ Must use USER_ROLE_CHANGED_KEY — not USER_REGISTERED_KEY
        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_ROLE_CHANGED_KEY, roleChangedEvent);
        verify(rabbitTemplate, never()).convertAndSend(
                AUTH_EXCHANGE, USER_REGISTERED_KEY, roleChangedEvent);
    }

    @Test
    void publishUserRoleChanged_whenRabbitThrows_shouldNotPropagateException() {
        doThrow(new RuntimeException("Connection timeout"))
                .when(rabbitTemplate).convertAndSend(
                        AUTH_EXCHANGE, USER_ROLE_CHANGED_KEY, roleChangedEvent);

        assertThatCode(() ->
                eventPublisher.publishUserRoleChanged(roleChangedEvent)
        ).doesNotThrowAnyException();

        verify(rabbitTemplate)
                .convertAndSend(AUTH_EXCHANGE, USER_ROLE_CHANGED_KEY, roleChangedEvent);
    }

    @Test
    void publishUserRoleChanged_toManager_shouldSendCorrectEvent() {
        UserRegisteredEvent toManager = UserRegisteredEvent.builder()
                .userId(3L)
                .email("emp@test.com")
                .role("MANAGER")
                .build();

        eventPublisher.publishUserRoleChanged(toManager);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_ROLE_CHANGED_KEY, toManager);
    }

    // ════════════════════════════════════════════════════════
    // publishUserDeleted
    // ✅ Sends to AUTH_EXCHANGE with USER_DELETED_KEY
    // ════════════════════════════════════════════════════════

    @Test
    void publishUserDeleted_shouldCallRabbitTemplateWithCorrectArgs() {
        eventPublisher.publishUserDeleted(userDeletedEvent);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE,
                USER_DELETED_KEY,
                userDeletedEvent
        );
    }

    @Test
    void publishUserDeleted_shouldUseDeletedKey_notRegisteredKey() {
        eventPublisher.publishUserDeleted(userDeletedEvent);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_DELETED_KEY, userDeletedEvent);
        verify(rabbitTemplate, never()).convertAndSend(
                AUTH_EXCHANGE, USER_REGISTERED_KEY, userDeletedEvent);
    }

    @Test
    void publishUserDeleted_whenRabbitThrows_shouldNotPropagateException() {
        doThrow(new RuntimeException("Queue not found"))
                .when(rabbitTemplate).convertAndSend(
                        AUTH_EXCHANGE, USER_DELETED_KEY, userDeletedEvent);

        assertThatCode(() ->
                eventPublisher.publishUserDeleted(userDeletedEvent)
        ).doesNotThrowAnyException();
    }

    @Test
    void publishUserDeleted_withDifferentUser_shouldSendCorrectUserId() {
        UserDeletedEvent anotherUser = UserDeletedEvent.builder()
                .userId(42L)
                .email("deleted@test.com")
                .build();

        eventPublisher.publishUserDeleted(anotherUser);

        verify(rabbitTemplate).convertAndSend(
                AUTH_EXCHANGE, USER_DELETED_KEY, anotherUser);
    }

    // ════════════════════════════════════════════════════════
    // Verify no cross-contamination between methods
    // ════════════════════════════════════════════════════════

    @Test
    void publishUserRegistered_shouldNotCallOtherPublishMethods() {
        eventPublisher.publishUserRegistered(userRegisteredEvent);

        // ✅ Only one interaction with the correct key
        verify(rabbitTemplate, times(1))
            .convertAndSend(AUTH_EXCHANGE, USER_REGISTERED_KEY, userRegisteredEvent);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishUserRoleChanged_shouldNotCallOtherPublishMethods() {
        eventPublisher.publishUserRoleChanged(roleChangedEvent);

        // ✅ Only one interaction with the correct key
        verify(rabbitTemplate, times(1))
            .convertAndSend(AUTH_EXCHANGE, USER_ROLE_CHANGED_KEY, roleChangedEvent);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishUserDeleted_shouldNotCallOtherPublishMethods() {
        eventPublisher.publishUserDeleted(userDeletedEvent);

        // ✅ Only one interaction with the correct key
        verify(rabbitTemplate, times(1))
            .convertAndSend(AUTH_EXCHANGE, USER_DELETED_KEY, userDeletedEvent);
        verifyNoMoreInteractions(rabbitTemplate);
    }

    // ════════════════════════════════════════════════════════
    // Event class tests — covers event package model classes
    // ════════════════════════════════════════════════════════

    @Test
    void userRegisteredEvent_builderShouldSetAllFields() {
        assertThat(userRegisteredEvent.getUserId()).isEqualTo(1L);
        assertThat(userRegisteredEvent.getEmail()).isEqualTo("john@test.com");
        assertThat(userRegisteredEvent.getFullName()).isEqualTo("John Doe");
        assertThat(userRegisteredEvent.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(userRegisteredEvent.getRole()).isEqualTo("EMPLOYEE");
    }

    @Test
    void userRegisteredEvent_noArgConstructor_shouldWork() {
        UserRegisteredEvent event = new UserRegisteredEvent();

        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getEmail()).isNull();
    }

    @Test
    void userRegisteredEvent_settersShouldWork() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUserId(5L);
        event.setEmail("test@test.com");
        event.setRole("ADMIN");

        assertThat(event.getUserId()).isEqualTo(5L);
        assertThat(event.getEmail()).isEqualTo("test@test.com");
        assertThat(event.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void userDeletedEvent_builderShouldSetAllFields() {
        // ✅ @Data on UserDeletedEvent — has equals, hashCode, toString
        assertThat(userDeletedEvent.getUserId()).isEqualTo(1L);
        assertThat(userDeletedEvent.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void userDeletedEvent_noArgConstructor_shouldWork() {
        UserDeletedEvent event = new UserDeletedEvent();

        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isNull();
    }

    @Test
    void userDeletedEvent_equalsShouldWork() {
        // ✅ @Data generates equals
        UserDeletedEvent event1 = UserDeletedEvent.builder()
                .userId(1L).email("john@test.com").build();
        UserDeletedEvent event2 = UserDeletedEvent.builder()
                .userId(1L).email("john@test.com").build();

        assertThat(event1).isEqualTo(event2);
    }
}