package com.qprint.objects.dto;

import com.qprint.objects.model.PrintObject;

import java.math.BigDecimal;
import java.util.UUID;

public record PrintObjectResponse(
        UUID objectId,
        String originalFilename,
        Integer pageCount,
        BigDecimal calculatedPrice,
        UploadPreferencesDto preferences
) {
    public static PrintObjectResponse fromEntity(PrintObject entity) {
        UploadPreferencesDto prefs = new UploadPreferencesDto(
                entity.getCopies(),
                entity.getColorMode(),
                entity.getSides(),
                entity.getPageRange(),
                entity.getPaperSize(),
                entity.getBinding(),
                entity.getSpecialInstructions()
        );
        return new PrintObjectResponse(entity.getId(), entity.getOriginalFilename(), entity.getPageCount(), entity.getCalculatedPrice(), prefs);
    }
}
