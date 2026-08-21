package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.ItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemDto {
    private Long id; // null for a new item

    @NotBlank
    private String description;

    private BigDecimal length;
    private BigDecimal width;

    @NotNull
    private BigDecimal quantity;

    @NotNull
    private ItemUnit unit;

    @NotNull
    private BigDecimal rate;

    // Sent by client for display; server always recomputes = quantity * rate before saving.
    private BigDecimal amount;

    private Integer sortOrder;
}
