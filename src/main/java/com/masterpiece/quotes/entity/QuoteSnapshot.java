package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

// An immutable copy of a quote exactly as it was shared with the customer (quote content plus
// the business details printed on the PDF at that moment). Later edits never change it.
@Entity
@Table(name = "quote_snapshots")
@Getter
@Setter
public class QuoteSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // SHA-256 of the quote's content - used to tell whether the quote changed since it was shared.
    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal roundedTotal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
}
