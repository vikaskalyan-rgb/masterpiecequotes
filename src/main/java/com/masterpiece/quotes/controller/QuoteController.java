package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.*;
import com.masterpiece.quotes.entity.QuoteStatus;
import com.masterpiece.quotes.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public List<QuoteSummaryDto> list(@RequestParam(required = false) QuoteStatus status,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(defaultValue = "false") boolean deleted) {
        return deleted ? quoteService.listDeleted() : quoteService.list(status, search);
    }

    @GetMapping("/{id}")
    public QuoteDetailDto getOne(@PathVariable Long id) {
        return quoteService.getById(id);
    }

    @PostMapping
    public ResponseEntity<QuoteDetailDto> create(@Valid @RequestBody QuoteRequest request) {
        QuoteDetailDto created = quoteService.create(request);
        return ResponseEntity.created(URI.create("/api/quotes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public QuoteDetailDto update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        return quoteService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public QuoteDetailDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return quoteService.updateStatus(id, request.status());
    }

    // Called right before the PDF goes to the customer: records exactly what was sent.
    @PostMapping("/{id}/share")
    public ShareResultDto share(@PathVariable Long id) {
        return quoteService.share(id);
    }

    @GetMapping("/{id}/snapshots")
    public List<SnapshotSummaryDto> snapshots(@PathVariable Long id) {
        return quoteService.listSnapshots(id);
    }

    @GetMapping(value = "/{id}/snapshots/{snapshotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String snapshot(@PathVariable Long id, @PathVariable Long snapshotId) {
        return quoteService.snapshotPayload(id, snapshotId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public QuoteDetailDto restore(@PathVariable Long id) {
        return quoteService.restore(id);
    }
}
