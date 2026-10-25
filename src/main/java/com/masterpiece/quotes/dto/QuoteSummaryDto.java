package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record QuoteSummaryDto(
        Long id,
        Integer quoteNumber,
        String customerName,
        String customerPhone,
        String customerAddress,
        LocalDate quoteDate,
        LocalDate validUntil,
        QuoteStatus status,
        BigDecimal subtotal,
        BigDecimal roundedTotal,
        boolean totalOverridden,
        Instant firstSentAt,
        Instant updatedAt,
        Instant deletedAt
) {
}
