package com.company.authservice.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.company.authservice.exception.InvalidOtpException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 3;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, ResetTokenEntry> resetTokenStore = new ConcurrentHashMap<>();

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.reset-token-expiry-minutes:10}")
    private int resetTokenExpiryMinutes;

    // ─── OTP Generation ───────────────────────────────────────

    public String generateOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        otpStore.put(email.toLowerCase(), new OtpEntry(otp, expiresAt, 0));
        log.info("OTP generated for: {}", email);

        return otp;
    }

    // ─── OTP Verification ─────────────────────────────────────

    public void verifyOtp(String email, String otp) {
        String key = email.toLowerCase();
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            throw new InvalidOtpException("No OTP was requested for this email");
        }

        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            otpStore.remove(key);
            throw new InvalidOtpException("OTP has expired. Please request a new one");
        }

        if (entry.attempts() >= MAX_ATTEMPTS) {
            otpStore.remove(key);
            throw new InvalidOtpException(
                    "Maximum OTP attempts exceeded. Please request a new one");
        }

        if (!entry.otp().equals(otp)) {
            // Increment attempts
            otpStore.put(key, new OtpEntry(
                    entry.otp(), entry.expiresAt(), entry.attempts() + 1));
            throw new InvalidOtpException("Invalid OTP");
        }

        // OTP is valid — remove it
        otpStore.remove(key);
        log.info("OTP verified successfully for: {}", email);
    }

    // ─── Reset Token ──────────────────────────────────────────

    public String generateResetToken(String email) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(resetTokenExpiryMinutes);

        resetTokenStore.put(token, new ResetTokenEntry(
                email.toLowerCase(), expiresAt));
        log.info("Reset token generated for: {}", email);

        return token;
    }

    public String validateResetToken(String token) {
        ResetTokenEntry entry = resetTokenStore.get(token);

        if (entry == null) {
            throw new InvalidOtpException("Invalid or expired reset token");
        }

        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            resetTokenStore.remove(token);
            throw new InvalidOtpException("Reset token has expired. Please restart the process");
        }

        // Token is valid — remove it (single-use)
        resetTokenStore.remove(token);
        log.info("Reset token validated for: {}", entry.email());

        return entry.email();
    }

    // ─── Scheduled Cleanup ────────────────────────────────────

    @Scheduled(fixedRate = 60_000) // every 60 seconds
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();

        long otpRemoved = otpStore.entrySet().removeIf(
                e -> now.isAfter(e.getValue().expiresAt())) ? 1 : 0;
        long tokenRemoved = resetTokenStore.entrySet().removeIf(
                e -> now.isAfter(e.getValue().expiresAt())) ? 1 : 0;

        if (otpRemoved > 0 || tokenRemoved > 0) {
            log.debug("Cleanup: removed expired OTPs and reset tokens");
        }
    }

    // ─── Internal Records ─────────────────────────────────────

    private record OtpEntry(String otp, LocalDateTime expiresAt, int attempts) {}
    private record ResetTokenEntry(String email, LocalDateTime expiresAt) {}
}
