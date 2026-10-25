package com.masterpiece.quotes.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SnapshotSummaryDto(Long id, Instant createdAt, BigDecimal roundedTotal) {
}
