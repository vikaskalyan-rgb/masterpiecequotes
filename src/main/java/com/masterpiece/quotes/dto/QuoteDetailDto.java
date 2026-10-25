package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuoteDetailDto(
        Long id,
        Integer quoteNumber,
        Long version,
        String customerName,
        String customerPhone,
        String customerAddress,
        LocalDate quoteDate,
        LocalDate validUntil,
        QuoteStatus status,
        BigDecimal subtotal,
        BigDecimal roundedTotal,
        boolean totalOverridden,
        // What the business rounding rule would produce from the current subtotal.
        BigDecimal suggestedTotal,
        String accessoriesDescription,
        BigDecimal accessoriesAmount,
        List<RoomDto> rooms,
        List<MaterialSpecItemDto> materialSpecItems,
        List<TermItemDto> termItems,
        Instant createdAt,
        Instant updatedAt,
        Instant firstSentAt,
        Instant lastSharedAt,
        // True when the quote was shared and has been edited since (the customer has an old copy).
        boolean changedSinceShared,
        long sharedVersionCount,
        Instant deletedAt
) {
}
