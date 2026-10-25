package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// Single-row table: the organisation details printed on every quote, plus pricing defaults.
@Entity
@Table(name = "business_profile")
@Getter
@Setter
public class BusinessProfile {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id = SINGLETON_ID;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 200)
    private String tagline;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 30)
    private String phonePrimary;

    @Column(length = 30)
    private String phoneSecondary;

    @Column(length = 120)
    private String email;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false, length = 120)
    private String signatory;

    @Column(nullable = false)
    private Integer roundingStep = 1000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TotalRounding roundingMode = TotalRounding.NEAREST;

    @Column(nullable = false)
    private Integer validityDays = 30;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
