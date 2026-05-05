package com.company.timesheetservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.company.timesheetservice.config.RabbitMQConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishTimesheetSubmitted(TimesheetStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                TIMESHEET_EXCHANGE, TIMESHEET_SUBMITTED_KEY, event);
            log.info("Published timesheet.submitted event for userId: {}", 
                event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish timesheet.submitted: {}", e.getMessage());
        }
    }

    public void publishTimesheetApproved(TimesheetStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                TIMESHEET_EXCHANGE, TIMESHEET_APPROVED_KEY, event);
            log.info("Published timesheet.approved event for userId: {}", 
                event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish timesheet.approved: {}", e.getMessage());
        }
    }

    public void publishTimesheetRejected(TimesheetStatusEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                TIMESHEET_EXCHANGE, TIMESHEET_REJECTED_KEY, event);
            log.info("Published timesheet.rejected event for userId: {}", 
                event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish timesheet.rejected: {}", e.getMessage());
        }
    }
}