package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room {

    @Id
    // Ids come 50 at a time from the table's own sequence (see V3), so a save inserts all rows
    // in one batch instead of one round trip per row.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rooms_ids")
    @SequenceGenerator(name = "rooms_ids", sequenceName = "rooms_id_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Item> items = new ArrayList<>();
}
