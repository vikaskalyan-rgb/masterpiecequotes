package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.DefaultTermItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DefaultTermItemRepository extends JpaRepository<DefaultTermItem, Long> {
    List<DefaultTermItem> findAllByOrderBySortOrderAsc();
}
