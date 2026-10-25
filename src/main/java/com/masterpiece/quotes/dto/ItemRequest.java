package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.ItemUnit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// NOS: quantity is the count. SQFT: quantity is length x width, unless areaAdjusted is true, in
// which case quantity is the billed area as typed (rounded to whole sqft, several pieces, odd shapes).
public record ItemRequest(
        @NotBlank @Size(max = 255) String description,
        @PositiveOrZero @DecimalMax("99999.99") BigDecimal length,
        @PositiveOrZero @DecimalMax("99999.99") BigDecimal width,
        @PositiveOrZero @DecimalMax("99999999.99") BigDecimal quantity,
        @NotNull ItemUnit unit,
        @NotNull @PositiveOrZero @DecimalMax("99999999.99") BigDecimal rate,
        Boolean areaAdjusted
) {
    public boolean isAreaAdjusted() {
        return Boolean.TRUE.equals(areaAdjusted);
    }
}
