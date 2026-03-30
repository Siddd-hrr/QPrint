package com.qprint.transactions.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

public record CompletedOrderEvent(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("userId") String userId,
        @JsonProperty("shopId") String shopId,
        @JsonProperty("items") JsonNode items,
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonAlias({"razorpayPaymentId", "paymentId"}) String razorpayPaymentId,
        @JsonProperty("completedAt") @JsonAlias("paidAt") Instant completedAt
) {
}
