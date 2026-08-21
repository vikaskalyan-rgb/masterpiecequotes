package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class QuoteSummaryDto {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private LocalDate quoteDate;
    private QuoteStatus status;
    private BigDecimal roundedTotal;
    private Instant updatedAt;
}
