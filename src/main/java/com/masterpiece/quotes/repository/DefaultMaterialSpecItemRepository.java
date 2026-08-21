package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.DefaultMaterialSpecItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultMaterialSpecItemRepository extends JpaRepository<DefaultMaterialSpecItem, Long> {
    List<DefaultMaterialSpecItem> findAllByOrderBySortOrderAsc();
}
