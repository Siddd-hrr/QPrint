package com.qprint.checkout.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutSummaryDto(
        UUID checkoutId,
        String razorpayOrderId,
        String razorpayPaymentId,
        BigDecimal amount,
        String currency,
        String status,
        List<CheckoutItemDto> items
) {
}
