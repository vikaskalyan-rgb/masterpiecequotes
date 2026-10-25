package com.masterpiece.quotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientErrorReport(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 8000) String stack,
        @Size(max = 500) String url,
        @Size(max = 500) String userAgent,
        @Size(max = 64) String release
) {
}
