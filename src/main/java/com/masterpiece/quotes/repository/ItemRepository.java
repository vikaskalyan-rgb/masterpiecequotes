package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Distinct past descriptions matching what he's typing, most-used first, for autocomplete.
    @Query("""
        SELECT i.description FROM Item i
        WHERE LOWER(i.description) LIKE LOWER(CONCAT(:prefix, '%'))
        GROUP BY i.description
        ORDER BY COUNT(i.description) DESC
        """)
    List<String> findSuggestions(@Param("prefix") String prefix, Pageable pageable);
}
