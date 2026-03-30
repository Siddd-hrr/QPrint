package com.qprint.otp.dto;

public record OtpVerificationResult(
        boolean valid,
        Integer attemptsLeft,
        String message
) {
}
