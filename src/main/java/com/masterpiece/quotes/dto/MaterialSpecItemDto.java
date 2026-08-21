package com.masterpiece.quotes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaterialSpecItemDto {
    private Long id; // null for a new row

    @NotBlank
    private String itemLabel;

    private String detail;
    private String brand;
    private Integer sortOrder;
}
