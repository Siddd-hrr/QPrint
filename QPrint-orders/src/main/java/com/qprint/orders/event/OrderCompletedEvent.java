package com.qprint.orders.event;

import com.qprint.orders.dto.OrderItemDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCompletedEvent(
        String orderId,
        String userId,
        String shopId,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        String razorpayPaymentId,
        Instant completedAt
) {
}
