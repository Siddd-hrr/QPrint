package com.qprint.orders.event;

import java.math.BigDecimal;

public record OrderConfirmedItem(
        String filename,
        BigDecimal price,
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
