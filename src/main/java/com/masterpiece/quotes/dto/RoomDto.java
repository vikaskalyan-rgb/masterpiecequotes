package com.masterpiece.quotes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoomDto {
    private Long id; // null for a new room

    @NotBlank
    private String name;

    private Integer sortOrder;

    @Valid
    private List<ItemDto> items = new ArrayList<>();
}
