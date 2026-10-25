package com.masterpiece.quotes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuoteRequest(
        @NotBlank @Size(max = 120) String customerName,
        // Any format; normalized to digits with country code (validated in QuoteService).
        @NotBlank @Size(max = 30) String customerPhone,
        @Size(max = 1000) String customerAddress,
        @NotNull LocalDate quoteDate,
        LocalDate validUntil,
        // Only read when totalOverridden is true; otherwise the server computes it.
        @PositiveOrZero @DecimalMax("9999999999.99") BigDecimal roundedTotal,
        Boolean totalOverridden,
        @Size(max = 255) String accessoriesDescription,
        @PositiveOrZero @DecimalMax("9999999999.99") BigDecimal accessoriesAmount,
        @Valid @Size(max = 50) List<RoomRequest> rooms,
        @Valid @Size(max = 50) List<MaterialSpecItemDto> materialSpecItems,
        @Valid @Size(max = 50) List<TermItemDto> termItems,
        // Required on update: the version this edit started from (optimistic locking).
        Long version,
        // Optional on create: makes retries safe (same id -> same quote, never a duplicate).
        @Size(max = 64) String clientRequestId
) {
    public boolean isTotalOverridden() {
        return Boolean.TRUE.equals(totalOverridden);
    }

    public List<RoomRequest> roomsOrEmpty() {
        return rooms == null ? List.of() : rooms;
    }

    public List<MaterialSpecItemDto> specsOrEmpty() {
        return materialSpecItems == null ? List.of() : materialSpecItems;
    }

    public List<TermItemDto> termsOrEmpty() {
        return termItems == null ? List.of() : termItems;
    }
}
