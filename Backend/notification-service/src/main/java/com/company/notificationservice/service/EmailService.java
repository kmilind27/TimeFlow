package com.company.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to Timesheet Management System";
        String body = String.format("Hello %s,%n%nWelcome to Timesheet Management System!%n%nYour account has been successfully registered.%n%nRegards,%nMilind,%n Admin Team, TMS",
                name);
        sendEmail(to, subject, body);
    }

    public void sendTimesheetStatusEmail(String to, String week, String status, String comment) {
        String subject = "Timesheet Status Update: " + status;
        String body = String.format("Hello,%n%nYour timesheet for week starting %s has been %s.%nReviewer Comment: %s%n%nRegards,%nTimesheet Team", week, status, comment != null ? comment : "N/A");
        sendEmail(to, subject, body);
    }

    public void sendLeaveStatusEmail(String to, String leaveType, String status, String comment) {
        String subject = "Leave Request Update: " + status;
        String body = String.format("Hello,%n%nYour %s Leave request has been %s.%nReviewer Comment: %s%n%nRegards,%nLeave Management Team", leaveType, status, comment != null ? comment : "N/A");
        sendEmail(to, subject, body);
    }

    public void sendOtpEmail(String to, String name, String otp) {
        String subject = "Password Reset OTP - Timesheet Management System";
        String body = String.format(
                "Hello %s,%n%n"
                + "You have requested to reset your password.%n%n"
                + "Your OTP is: %s%n%n"
                + "This OTP is valid for 5 minutes. Do not share it with anyone.%n"
                + "If you did not request this, please ignore this email.%n%n"
                + "Regards,%nAdmin Team, TMS",
                name, otp);
        sendEmail(to, subject, body);
    }
}
