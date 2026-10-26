package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "term_items")
@Getter
@Setter
public class TermItem {

    @Id
    // Ids come 50 at a time from the table's own sequence (see V3), so a save inserts all rows
    // in one batch instead of one round trip per row.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "term_items_ids")
    @SequenceGenerator(name = "term_items_ids", sequenceName = "term_items_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
