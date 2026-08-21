package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "items")
@Getter
@Setter
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // e.g. "Wardrobe Framed" - dimensions are appended for display, e.g. "Wardrobe Framed (7X4)"
    @Column(nullable = false)
    private String description;

    // Nullable: NOS-unit items (like flat-count accessories) may not have L x W
    @Column(precision = 10, scale = 2)
    private BigDecimal length;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    // For SQFT items this is length * width (computed client-side, trusted here).
    // For NOS items this is a plain count entered directly.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemUnit unit = ItemUnit.SQFT;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    // Always recomputed server-side as quantity * rate before saving.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
