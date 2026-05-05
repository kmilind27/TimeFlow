package com.company.leaveservice.event;

import com.company.leaveservice.dto.UserRegisteredEvent;
import com.company.leaveservice.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.company.leaveservice.config.RabbitMQConfig.USER_REGISTERED_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final LeaveService leaveService;

    @RabbitListener(queues = USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event for user: {} (ID: {})", 
                event.getEmail(), event.getUserId());
        try {
            leaveService.initializeLeaveBalances(event.getUserId());
            log.info("Successfully initialized leave balances for user ID: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to initialize leave balances for user ID: {}. Error: {}", 
                    event.getUserId(), e.getMessage());
        }
    }
}
