package com.qprint.checkout.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderConfirmedEvent(
        String orderId,
        String userId,
        String shopId,
        List<OrderConfirmedItem> items,
        BigDecimal totalAmount,
        String razorpayPaymentId,
        Instant paidAt,
        String checkoutId,
        String currency
) {
}
