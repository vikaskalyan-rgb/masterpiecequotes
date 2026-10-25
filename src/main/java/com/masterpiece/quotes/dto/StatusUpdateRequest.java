package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull QuoteStatus status) {
}
