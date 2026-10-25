package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.ItemUnit;

import java.math.BigDecimal;

public record ItemDto(
        Long id,
        String description,
        BigDecimal length,
        BigDecimal width,
        BigDecimal quantity,
        ItemUnit unit,
        BigDecimal rate,
        BigDecimal amount
) {
}
