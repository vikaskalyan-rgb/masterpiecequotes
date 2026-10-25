package com.masterpiece.quotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TermItemDto(@NotBlank @Size(max = 2000) String text) {
}
