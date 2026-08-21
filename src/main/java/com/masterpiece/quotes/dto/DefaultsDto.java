package com.masterpiece.quotes.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DefaultsDto {
    @Valid
    private List<MaterialSpecItemDto> materialSpecItems = new ArrayList<>();

    @Valid
    private List<TermItemDto> termItems = new ArrayList<>();
}
