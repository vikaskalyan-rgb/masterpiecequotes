package com.masterpiece.quotes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "default_material_spec_items")
@Getter
@Setter
public class DefaultMaterialSpecItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemLabel;

    private String detail;

    private String brand;

    @Column(nullable = false)
    private Integer sortOrder = 0;
}
