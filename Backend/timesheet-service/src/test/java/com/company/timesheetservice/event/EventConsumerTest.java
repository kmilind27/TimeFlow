package com.company.timesheetservice.event;

import com.company.timesheetservice.service.TimesheetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumer Unit Tests")
class EventConsumerTest {

    @Mock private TimesheetService timesheetService;
    @InjectMocks private EventConsumer eventConsumer;

    @Test
    @DisplayName("consumeUserDeletedEvent — delegates softDelete with correct userId")
    void consumeUserDeleted_CallsSoftDelete() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(5L).email("user@example.com").build();

        eventConsumer.consumeUserDeletedEvent(event);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(timesheetService).softDeleteUserData(captor.capture());
        assertEquals(5L, captor.getValue());
    }

    @Test
    @DisplayName("consumeUserDeletedEvent — swallows service exception, does not propagate")
    void consumeUserDeleted_SwallowsException() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(5L).email("user@example.com").build();

        doThrow(new RuntimeException("DB error"))
                .when(timesheetService).softDeleteUserData(5L);

        assertDoesNotThrow(() -> eventConsumer.consumeUserDeletedEvent(event));
    }

    @Test
    @DisplayName("consumeUserDeletedEvent — only the target userId is soft-deleted")
    void consumeUserDeleted_CorrectUserIdIsolated() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .userId(99L).email("other@example.com").build();

        eventConsumer.consumeUserDeletedEvent(event);

        verify(timesheetService).softDeleteUserData(99L);
        verify(timesheetService, never()).softDeleteUserData(5L);
    }
}