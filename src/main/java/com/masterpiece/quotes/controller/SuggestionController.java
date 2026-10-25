package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.CustomerSuggestionDto;
import com.masterpiece.quotes.dto.ItemSuggestionDto;
import com.masterpiece.quotes.service.QuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SuggestionController {

    private final QuoteService quoteService;

    public SuggestionController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    // Item descriptions typed before, with the unit they were used with. Rates are never suggested:
    // they're negotiated per customer.
    @GetMapping("/api/items/suggestions")
    public List<ItemSuggestionDto> items(@RequestParam(defaultValue = "") String q) {
        return quoteService.itemSuggestions(q);
    }

    // Repeat customers: name, phone and address from their most recent quote.
    @GetMapping("/api/customers/suggestions")
    public List<CustomerSuggestionDto> customers(@RequestParam(defaultValue = "") String q) {
        return quoteService.customerSuggestions(q);
    }
}
