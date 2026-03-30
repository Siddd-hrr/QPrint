package com.qprint.cart.dto;

import com.qprint.cart.model.PrintObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemDto(
        UUID objectId,
        String filename,
        BigDecimal price,
        String colorMode,
        String sides,
        String paperSize,
        String binding,
        Integer copies,
        Integer pageCount,
        String pageRange,
        Instant addedAt
) {
    public static CartItemDto from(PrintObject obj, BigDecimal price, Instant addedAt) {
        return new CartItemDto(
                obj.getId(),
                obj.getOriginalFilename(),
                price,
                obj.getColorMode().name(),
                obj.getSides().name(),
                obj.getPaperSize().name(),
                obj.getBinding().name(),
                obj.getCopies(),
                obj.getPageCount(),
                obj.getPageRange(),
                addedAt
        );
    }
}
