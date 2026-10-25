package com.masterpiece.quotes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DefaultsDto(
        @Valid @Size(max = 50) List<MaterialSpecItemDto> materialSpecItems,
        @Valid @Size(max = 50) List<TermItemDto> termItems
) {
    public List<MaterialSpecItemDto> specsOrEmpty() {
        return materialSpecItems == null ? List.of() : materialSpecItems;
    }

    public List<TermItemDto> termsOrEmpty() {
        return termItems == null ? List.of() : termItems;
    }
}
