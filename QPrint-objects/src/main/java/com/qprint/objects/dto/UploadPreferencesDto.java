package com.qprint.objects.dto;

import com.qprint.objects.model.Binding;
import com.qprint.objects.model.ColorMode;
import com.qprint.objects.model.PaperSize;
import com.qprint.objects.model.Sides;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UploadPreferencesDto(
        @NotNull @Min(1) @Max(100) Integer copies,
        @NotNull ColorMode colorMode,
        @NotNull Sides sides,
        @Size(max = 100) @Pattern(regexp = "^(ALL|[0-9,\\-\s]+)$", message = "Invalid page range format") String pageRange,
        @NotNull PaperSize paperSize,
        @NotNull Binding binding,
        @Size(max = 500) String specialInstructions
) {
}
