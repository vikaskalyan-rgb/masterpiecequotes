package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "material_spec_items")
@Getter
@Setter
public class MaterialSpecItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    // e.g. "Plywood - Kitchen Base"
    @Column(nullable = false)
    private String itemLabel;

    // e.g. "BWP 710 Grade, Moisture Resistant"
    private String detail;

    // e.g. "GAMA Gurjan Hardwood"
    private String brand;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
