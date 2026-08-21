package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(nullable = false)
    private String customerName;

    // Stored with country code, digits only, e.g. "919994445388" - used to build wa.me links
    @Column(nullable = false)
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String customerAddress;

    @Column(nullable = false)
    private LocalDate quoteDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.DRAFT;

    // Sum of every item amount + accessories amount. Recomputed server-side on every save.
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // The final, editable amount shown to the customer (may differ from subtotal - rounding / discount)
    @Column(precision = 12, scale = 2)
    private BigDecimal roundedTotal = BigDecimal.ZERO;

    private String accessoriesDescription;

    @Column(precision = 12, scale = 2)
    private BigDecimal accessoriesAmount = BigDecimal.ZERO;

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

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
