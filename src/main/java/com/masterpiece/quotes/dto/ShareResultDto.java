package com.masterpiece.quotes.dto;

public record ShareResultDto(SnapshotSummaryDto snapshot, boolean newVersion, QuoteDetailDto quote) {
}
