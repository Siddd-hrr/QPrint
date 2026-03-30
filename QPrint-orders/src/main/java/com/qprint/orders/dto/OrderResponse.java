package com.qprint.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String checkoutId,
        String paymentId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        String otp,
        Instant createdAt,
        List<OrderItemDto> items
) {
}
