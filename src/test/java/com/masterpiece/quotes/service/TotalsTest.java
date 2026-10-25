package com.masterpiece.quotes.service;

import com.masterpiece.quotes.entity.ItemUnit;
import com.masterpiece.quotes.entity.TotalRounding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TotalsTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void sqftQuantityIsLengthTimesWidthAndIgnoresTypedQuantity() {
        assertThat(Totals.quantity(ItemUnit.SQFT, d("7"), d("4"), d("999"), false)).isEqualByComparingTo("28");
        assertThat(Totals.quantity(ItemUnit.SQFT, d("7.25"), d("3.33"), null, false)).isEqualByComparingTo("24.14");
    }

    @Test
    void sqftWithoutBothDimensionsUsesTheTypedArea() {
        // Real production data: "(2X2)(1X7)" entered as 11 sqft with no single length/width.
        assertThat(Totals.quantity(ItemUnit.SQFT, null, null, d("11"), false)).isEqualByComparingTo("11");
        assertThat(Totals.quantity(ItemUnit.SQFT, d("7"), null, d("28"), false)).isEqualByComparingTo("28");
        assertThat(Totals.quantity(ItemUnit.SQFT, null, null, null, false)).isEqualByComparingTo("0");
    }

    @Test
    void adjustedBilledAreaIsKeptEvenWithDimensions() {
        // Real production data: 2.5 x 5.5 = 13.75 billed as 14; "2 Nos" of 1.5 x 1.5 billed as 4.5.
        assertThat(Totals.quantity(ItemUnit.SQFT, d("2.5"), d("5.5"), d("14"), true)).isEqualByComparingTo("14");
        assertThat(Totals.quantity(ItemUnit.SQFT, d("1.5"), d("1.5"), d("4.5"), true)).isEqualByComparingTo("4.5");
    }

    @Test
    void nosQuantityIsTheCountEntered() {
        assertThat(Totals.quantity(ItemUnit.NOS, d("7"), d("4"), d("3"), false)).isEqualByComparingTo("3");
    }

    @Test
    void amountRoundsHalfUpToPaise() {
        assertThat(Totals.amount(d("24.14"), d("1234.55"))).isEqualByComparingTo("29802.04");
    }

    @Test
    void nearestRoundingCanGoDownOrUp() {
        assertThat(Totals.suggestedTotal(d("124499"), TotalRounding.NEAREST, 1000)).isEqualByComparingTo("124000");
        assertThat(Totals.suggestedTotal(d("124500"), TotalRounding.NEAREST, 1000)).isEqualByComparingTo("125000");
    }

    @Test
    void upRoundingNeverGoesBelowSubtotal() {
        assertThat(Totals.suggestedTotal(d("124001"), TotalRounding.UP, 1000)).isEqualByComparingTo("125000");
        assertThat(Totals.suggestedTotal(d("124000"), TotalRounding.UP, 1000)).isEqualByComparingTo("124000");
    }

    @Test
    void noRoundingKeepsSubtotal() {
        assertThat(Totals.suggestedTotal(d("124499.50"), TotalRounding.NONE, 1000)).isEqualByComparingTo("124499.50");
    }
}
