package com.qprint.orders.dto;

import com.qprint.orders.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID objectId,
        String filename,
        BigDecimal price,
        String colorMode,
        String sides,
        String paperSize,
        String binding,
        Integer copies,
        Integer pageCount,
        String pageRange
) {
    public static OrderItemDto from(OrderItem item) {
        return new OrderItemDto(
                item.getObjectId(),
                item.getFilename(),
                item.getPrice(),
                item.getColorMode() != null ? item.getColorMode().name() : null,
                item.getSides() != null ? item.getSides().name() : null,
                item.getPaperSize() != null ? item.getPaperSize().name() : null,
                item.getBinding() != null ? item.getBinding().name() : null,
                item.getCopies(),
                item.getPageCount(),
                item.getPageRange()
        );
    }
}
