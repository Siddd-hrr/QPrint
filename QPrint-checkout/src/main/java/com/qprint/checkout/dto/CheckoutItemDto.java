package com.qprint.checkout.dto;

import com.qprint.checkout.model.CheckoutItem;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutItemDto(
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
    public static CheckoutItemDto from(CheckoutItem item) {
        return new CheckoutItemDto(
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
