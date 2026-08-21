package com.masterpiece.quotes.dto;

import com.masterpiece.quotes.entity.QuoteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QuoteRequest {

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerPhone;

    private String customerAddress;

    @NotNull
    private LocalDate quoteDate;

    private QuoteStatus status; // defaults to DRAFT if omitted on create

    // The final editable total shown to the customer. Frontend suggests a rounded value; this is what gets saved.
    private BigDecimal roundedTotal;

    private String accessoriesDescription;

    private BigDecimal accessoriesAmount;

    @Valid
    private List<RoomDto> rooms = new ArrayList<>();

    @Valid
    private List<MaterialSpecItemDto> materialSpecItems = new ArrayList<>();

    @Valid
    private List<TermItemDto> termItems = new ArrayList<>();
}
