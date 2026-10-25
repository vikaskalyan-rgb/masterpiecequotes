package com.masterpiece.quotes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Complete, restorable export: every quote (including deleted ones), every shared version,
// the default templates and the business profile.
public record BackupDto(
        String format,
        int formatVersion,
        Instant exportedAt,
        BusinessProfileDto business,
        DefaultsDto defaults,
        List<QuoteDetailDto> quotes,
        List<SnapshotBackup> snapshots
) {
    public static final String FORMAT = "masterpiece-quotes-backup";
    public static final int FORMAT_VERSION = 1;

    public record SnapshotBackup(Integer quoteNumber, Instant createdAt, String contentHash,
                                 BigDecimal roundedTotal, String payload) {
    }
}
