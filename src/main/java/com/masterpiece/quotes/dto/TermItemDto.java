package com.masterpiece.quotes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermItemDto {
    private Long id; // null for a new row

    @NotBlank
    private String text;

    private Integer sortOrder;
}
