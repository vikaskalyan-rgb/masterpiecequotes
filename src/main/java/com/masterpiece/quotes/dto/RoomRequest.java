package com.masterpiece.quotes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoomRequest(
        @NotBlank @Size(max = 120) String name,
        @Valid @Size(max = 200) List<ItemRequest> items
) {
    public List<ItemRequest> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }
}
