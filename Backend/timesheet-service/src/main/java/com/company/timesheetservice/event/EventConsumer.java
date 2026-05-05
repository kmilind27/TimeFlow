package com.company.timesheetservice.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.company.timesheetservice.config.RabbitMQConfig;
import com.company.timesheetservice.service.TimesheetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final TimesheetService timesheetService;

    @RabbitListener(queues = RabbitMQConfig.USER_DELETED_QUEUE)
    public void consumeUserDeletedEvent(UserDeletedEvent event) {
        log.info("Received user.deleted event for userId: {}", event.getUserId());
        try {
            timesheetService.softDeleteUserData(event.getUserId());
            log.info("Successfully soft deleted timesheet data for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to soft delete timesheet data for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
