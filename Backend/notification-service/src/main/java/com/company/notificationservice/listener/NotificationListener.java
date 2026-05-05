package com.company.notificationservice.listener;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.company.notificationservice.service.EmailService;
import com.company.notificationservice.event.LeaveStatusEvent;
import com.company.notificationservice.event.OtpEvent;
import com.company.notificationservice.event.TimesheetStatusEvent;
import com.company.notificationservice.event.UserRegisteredEvent;

import static com.company.notificationservice.config.RabbitMQConfig.*;
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    // ─── Auth Events ───────────────────────────────────────────

    @RabbitListener(queues = USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("NEW USER REGISTERED: {}", event.getEmail());
        emailService.sendWelcomeEmail(event.getEmail(), event.getFullName());
    }

    @RabbitListener(queues = USER_ROLE_CHANGED_QUEUE)
    public void handleUserRoleChanged(UserRegisteredEvent event) {
        log.info("USER ROLE CHANGED: {} → {}", event.getEmail(), event.getRole());
        emailService.sendEmail(event.getEmail(), "Role Updated", "Your role has been updated to: " + event.getRole());
    }

    @RabbitListener(queues = OTP_REQUESTED_QUEUE)
    public void handleOtpRequested(OtpEvent event) {
        log.info("OTP REQUESTED for: {}", event.getEmail());
        emailService.sendOtpEmail(event.getEmail(), event.getFullName(), event.getOtp());
    }

    // ─── Timesheet Events ──────────────────────────────────────

    @RabbitListener(queues = TIMESHEET_SUBMITTED_QUEUE)
    public void handleTimesheetSubmitted(TimesheetStatusEvent event) {
        log.info("TIMESHEET SUBMITTED for user: {}", event.getUserEmail());
        // For now, sending a copy to user, in real scenario this would notify manager
        emailService.sendTimesheetStatusEmail(event.getUserEmail(), event.getWeekStart(), "SUBMITTED", null);
    }

    @RabbitListener(queues = TIMESHEET_APPROVED_QUEUE)
    public void handleTimesheetApproved(TimesheetStatusEvent event) {
        log.info("TIMESHEET APPROVED for user: {}", event.getUserEmail());
        emailService.sendTimesheetStatusEmail(event.getUserEmail(), event.getWeekStart(), "APPROVED", event.getReviewComment());
    }

    @RabbitListener(queues = TIMESHEET_REJECTED_QUEUE)
    public void handleTimesheetRejected(TimesheetStatusEvent event) {
        log.info("TIMESHEET REJECTED for user: {}", event.getUserEmail());
        emailService.sendTimesheetStatusEmail(event.getUserEmail(), event.getWeekStart(), "REJECTED", event.getReviewComment());
    }

    // ─── Leave Events ──────────────────────────────────────────

    @RabbitListener(queues = LEAVE_APPLIED_QUEUE)
    public void handleLeaveApplied(LeaveStatusEvent event) {
        log.info("LEAVE APPLIED for user: {}", event.getUserEmail());
        emailService.sendLeaveStatusEmail(event.getUserEmail(), event.getLeaveType(), "APPLIED", null);
    }

    @RabbitListener(queues = LEAVE_APPROVED_QUEUE)
    public void handleLeaveApproved(LeaveStatusEvent event) {
        log.info("LEAVE APPROVED for user: {}", event.getUserEmail());
        emailService.sendLeaveStatusEmail(event.getUserEmail(), event.getLeaveType(), "APPROVED", event.getReviewComment());
    }

    @RabbitListener(queues = LEAVE_REJECTED_QUEUE)
    public void handleLeaveRejected(LeaveStatusEvent event) {
        log.info("LEAVE REJECTED for user: {}", event.getUserEmail());
        emailService.sendLeaveStatusEmail(event.getUserEmail(), event.getLeaveType(), "REJECTED", event.getReviewComment());
    }

    @RabbitListener(queues = LEAVE_CANCELLED_QUEUE)
    public void handleLeaveCancelled(LeaveStatusEvent event) {
        log.info("LEAVE CANCELLED for user: {}", event.getUserEmail());
        emailService.sendLeaveStatusEmail(event.getUserEmail(), event.getLeaveType(), "CANCELLED", null);
    }
}