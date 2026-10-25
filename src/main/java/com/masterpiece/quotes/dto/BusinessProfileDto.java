package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.TotalRounding;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessProfileDto(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 200) String tagline,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 30) String phonePrimary,
        @Size(max = 30) String phoneSecondary,
        @Email @Size(max = 120) String email,
        // Indian GSTIN: 15 characters (2-digit state code, PAN, entity code, 'Z', checksum).
        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "must be a valid 15-character GSTIN")
        String gstin,
        @NotBlank @Size(max = 120) String signatory,
        @NotNull Integer roundingStep,
        @NotNull TotalRounding roundingMode,
        @NotNull @Min(1) @Max(365) Integer validityDays
) {
}
