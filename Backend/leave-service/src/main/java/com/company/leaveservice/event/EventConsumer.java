package com.company.leaveservice.event;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.company.leaveservice.config.RabbitMQConfig;
import com.company.leaveservice.service.LeaveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final LeaveService leaveService;

    @RabbitListener(queues = RabbitMQConfig.USER_DELETED_QUEUE)
    public void consumeUserDeletedEvent(UserDeletedEvent event) {
        log.info("Received user.deleted event for userId: {}", event.getUserId());
        try {
            leaveService.softDeleteUserData(event.getUserId());
            log.info("Successfully soft deleted leave data for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to soft delete leave data for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
