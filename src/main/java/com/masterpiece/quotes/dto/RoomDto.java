package com.masterpiece.quotes.dto;

import java.math.BigDecimal;
import java.util.List;

public record RoomDto(Long id, String name, BigDecimal total, List<ItemDto> items) {
}
