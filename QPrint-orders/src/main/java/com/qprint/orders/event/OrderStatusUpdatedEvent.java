package com.qprint.orders.event;

public record OrderStatusUpdatedEvent(
        String orderId,
        String newStatus
) {
}
