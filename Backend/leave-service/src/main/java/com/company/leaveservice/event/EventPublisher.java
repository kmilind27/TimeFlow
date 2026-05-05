package com.company.leaveservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.company.leaveservice.config.RabbitMQConfig.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishLeaveApplied(LeaveStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                LEAVE_EXCHANGE, LEAVE_APPLIED_KEY, event);
            log.info("Published leave.applied event for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish leave.applied: {}", e.getMessage());
        }
    }

    public void publishLeaveApproved(LeaveStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                LEAVE_EXCHANGE, LEAVE_APPROVED_KEY, event);
            log.info("Published leave.approved event for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish leave.approved: {}", e.getMessage());
        }
    }

    public void publishLeaveRejected(LeaveStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                LEAVE_EXCHANGE, LEAVE_REJECTED_KEY, event);
            log.info("Published leave.rejected event for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish leave.rejected: {}", e.getMessage());
        }
    }

    public void publishLeaveCancelled(LeaveStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                LEAVE_EXCHANGE, LEAVE_CANCELLED_KEY, event);
            log.info("Published leave.cancelled event for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish leave.cancelled: {}", e.getMessage());
        }
    }
}