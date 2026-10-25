package com.masterpiece.quotes.service;

import com.masterpiece.quotes.entity.ItemUnit;
import com.masterpiece.quotes.entity.TotalRounding;

import java.math.BigDecimal;
import java.math.RoundingMode;

// The only place quote arithmetic is defined. The frontend shows a live preview with the same
// rules, but what is saved (and printed) always comes from here.
public final class Totals {

    private Totals() {
    }

    public static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public static BigDecimal money(BigDecimal value) {
        return nz(value).setScale(2, RoundingMode.HALF_UP);
    }

    // SQFT: length x width, unless the billed area was adjusted by hand (real quotes round 13.75 to
    // 14 sqft, bill "2 Nos" of 1.5 x 1.5 as 4.5, or enter "(2X2)(1X7)" as 11) or there are no
    // dimensions; then the area as typed. NOS: the count.
    public static BigDecimal quantity(ItemUnit unit, BigDecimal length, BigDecimal width, BigDecimal enteredQuantity,
                                      boolean areaAdjusted) {
        if (unit == ItemUnit.SQFT && !areaAdjusted && length != null && width != null) {
            return length.multiply(width).setScale(2, RoundingMode.HALF_UP);
        }
        return money(enteredQuantity);
    }

    public static BigDecimal amount(BigDecimal quantity, BigDecimal rate) {
        return nz(quantity).multiply(nz(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal suggestedTotal(BigDecimal subtotal, TotalRounding mode, int step) {
        BigDecimal s = money(subtotal);
        if (mode == TotalRounding.NONE || step <= 1) {
            return s;
        }
        BigDecimal stepValue = BigDecimal.valueOf(step);
        RoundingMode rounding = mode == TotalRounding.UP ? RoundingMode.CEILING : RoundingMode.HALF_UP;
        return s.divide(stepValue, 0, rounding).multiply(stepValue).setScale(2, RoundingMode.HALF_UP);
    }
}
