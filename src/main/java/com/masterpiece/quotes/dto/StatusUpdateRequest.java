package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull
    private QuoteStatus status;
}
