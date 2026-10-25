package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.ItemUnit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// quantity is only read for NOS items; SQFT quantity is always recomputed as length x width.
public record ItemRequest(
        @NotBlank @Size(max = 255) String description,
        @PositiveOrZero @DecimalMax("99999.99") BigDecimal length,
        @PositiveOrZero @DecimalMax("99999.99") BigDecimal width,
        @PositiveOrZero @DecimalMax("99999999.99") BigDecimal quantity,
        @NotNull ItemUnit unit,
        @NotNull @PositiveOrZero @DecimalMax("99999999.99") BigDecimal rate
) {
}
