package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter
@Setter
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-facing reference printed on the PDF (MP-0042). Assigned by a database sequence.
    @Generated(event = EventType.INSERT)
    @Column(name = "quote_number", nullable = false, insertable = false, updatable = false)
    private Integer quoteNumber;

    // Optimistic lock: a save based on a stale copy (e.g. a second phone) is rejected with 409.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private String customerName;

    // Digits only, with country code, e.g. "919994445388" - normalized server-side on every save.
    @Column(nullable = false)
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String customerAddress;

    @Column(nullable = false)
    private LocalDate quoteDate;

    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.DRAFT;

    // Sum of every item amount + accessories amount. Always computed server-side.
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // The amount shown to the customer. Computed from the subtotal by the business rounding rule,
    // unless totalOverridden is true, in which case it is the value typed in the builder.
    @Column(precision = 12, scale = 2)
    private BigDecimal roundedTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean totalOverridden = false;

    private String accessoriesDescription;

    @Column(precision = 12, scale = 2)
    private BigDecimal accessoriesAmount = BigDecimal.ZERO;

    // Client-generated id sent with "create" so a retried request never makes a duplicate quote.
    @Column(length = 64)
    private String clientRequestId;

    private Instant firstSentAt;

    // Soft delete: hidden everywhere, restorable from Settings.
    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Room> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MaterialSpecItem> materialSpecItems = new ArrayList<>();

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TermItem> termItems = new ArrayList<>();

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
