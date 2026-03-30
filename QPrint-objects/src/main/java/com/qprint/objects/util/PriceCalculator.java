package com.qprint.objects.util;

import com.qprint.objects.model.Binding;
import com.qprint.objects.model.ColorMode;
import com.qprint.objects.model.Sides;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

    private PriceCalculator() {}

    public static BigDecimal calculate(int pageCount, int copies, Sides sides, ColorMode colorMode, Binding binding) {
        BigDecimal basePrice = colorMode == ColorMode.COLOR ? BigDecimal.valueOf(8.00) : BigDecimal.valueOf(1.50);

        int effectivePages = pageCount * copies;
        BigDecimal multiplier = BigDecimal.ONE;
        if (sides == Sides.DOUBLE) {
            effectivePages = (int) Math.ceil(pageCount / 2.0) * copies;
            multiplier = BigDecimal.valueOf(0.9);
        }

        BigDecimal bindingCost = binding == Binding.SPIRAL ? BigDecimal.valueOf(15) : BigDecimal.ZERO;

        return basePrice
                .multiply(BigDecimal.valueOf(effectivePages))
                .multiply(multiplier)
                .add(bindingCost)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
