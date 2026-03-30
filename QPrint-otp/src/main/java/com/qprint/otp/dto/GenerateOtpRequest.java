package com.qprint.otp.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateOtpRequest(
        @NotBlank String orderId,
        @NotBlank String userId
) {
}
