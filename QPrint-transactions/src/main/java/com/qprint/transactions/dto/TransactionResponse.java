package com.qprint.transactions.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID orderId,
        String shopId,
        BigDecimal totalAmount,
        String razorpayPaymentId,
        Instant completedAt,
        Instant createdAt,
        JsonNode items
) {
}
