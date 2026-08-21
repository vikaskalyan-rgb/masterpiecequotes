package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QuoteDetailDto {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private LocalDate quoteDate;
    private QuoteStatus status;
    private BigDecimal subtotal;
    private BigDecimal roundedTotal;
    private String accessoriesDescription;
    private BigDecimal accessoriesAmount;
    private Instant createdAt;
    private Instant updatedAt;

    private List<RoomDto> rooms = new ArrayList<>();
    private List<MaterialSpecItemDto> materialSpecItems = new ArrayList<>();
    private List<TermItemDto> termItems = new ArrayList<>();
}
