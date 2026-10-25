package com.masterpiece.quotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialSpecItemDto(
        @NotBlank @Size(max = 255) String itemLabel,
        @Size(max = 255) String detail,
        @Size(max = 255) String brand
) {
}
