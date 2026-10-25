package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Past descriptions starting with what's being typed, most-used first, with the unit each
    // was used with (Object[]: description, unit, count). The prefix is LIKE-escaped by the caller.
    @Query("""
        SELECT i.description, i.unit, COUNT(i) FROM Item i
        WHERE i.room.quote.deletedAt IS NULL
        AND LOWER(i.description) LIKE LOWER(CONCAT(:prefix, '%')) ESCAPE '\\'
        GROUP BY i.description, i.unit
        ORDER BY COUNT(i) DESC
        """)
    List<Object[]> findSuggestions(@Param("prefix") String prefix, Pageable pageable);
}
