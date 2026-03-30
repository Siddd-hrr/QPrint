package com.qprint.otp.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank String orderId,
        @NotBlank String otp
) {
}
