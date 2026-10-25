package com.masterpiece.quotes.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsDto(
        Map<String, Long> statusCounts,
        // Sum of final totals of quotes that were sent (SENT, ACCEPTED or REJECTED). Drafts never count.
        BigDecimal quotedValue,
        long quotedCount,
        BigDecimal wonValue,
        long wonCount,
        // Accepted / (accepted + rejected); null until at least one quote has been decided.
        Double conversionRate,
        BigDecimal averageQuote,
        List<Month> months,
        List<Customer> topCustomers,
        long staleDrafts
) {
    public record Month(String month, BigDecimal quoted, BigDecimal won, long count) {
    }

    // Customers are identified by phone number, so the same person typed two ways counts once.
    public record Customer(String name, String phone, BigDecimal value, long quotes) {
    }
}
