package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.QuoteDetailDto;
import com.masterpiece.quotes.dto.QuoteRequest;
import com.masterpiece.quotes.dto.QuoteSummaryDto;
import com.masterpiece.quotes.dto.StatusUpdateRequest;
import com.masterpiece.quotes.entity.QuoteStatus;
import com.masterpiece.quotes.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public List<QuoteSummaryDto> list(
            @RequestParam(required = false) QuoteStatus status,
            @RequestParam(required = false) String search) {
        return quoteService.list(status, search);
    }

    @GetMapping("/{id}")
    public QuoteDetailDto getOne(@PathVariable Long id) {
        return quoteService.getById(id);
    }

    @PostMapping
    public ResponseEntity<QuoteDetailDto> create(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(quoteService.create(request));
    }

    @PutMapping("/{id}")
    public QuoteDetailDto update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        return quoteService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public QuoteDetailDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return quoteService.updateStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
