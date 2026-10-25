package com.masterpiece.quotes.dto;

// What a shared version stores: the quote and the business details printed on it at that time.
public record SnapshotPayload(QuoteDetailDto quote, BusinessProfileDto business) {
}
