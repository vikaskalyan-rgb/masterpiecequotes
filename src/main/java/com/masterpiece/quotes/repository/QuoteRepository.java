package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.Quote;
import com.masterpiece.quotes.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    @Query("""
        SELECT q FROM Quote q
        WHERE (:status IS NULL OR q.status = :status)
        AND (:search IS NULL OR LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY q.updatedAt DESC
        """)
    List<Quote> search(@Param("status") QuoteStatus status, @Param("search") String search);
}
