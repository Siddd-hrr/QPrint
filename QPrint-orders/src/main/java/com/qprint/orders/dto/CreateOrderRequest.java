package com.qprint.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String checkoutId,
        String paymentId,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotEmpty List<OrderItemPayload> items
) {
    public record OrderItemPayload(
            @NotBlank String filename,
            @NotNull BigDecimal price,
            Integer copies,
            Integer pageCount,
            String pageRange,
            String colorMode,
            String sides,
            String paperSize,
            String binding,
            String objectId
    ) {
    }
}
