package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.Quote;
import com.masterpiece.quotes.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    // "search" must always be a non-null string (use "" to match everyone) - binding a null
    // parameter into LOWER()/CONCAT() makes Postgres/JDBC infer it as bytea and throw
    // "function lower(bytea) does not exist".
    //
    // Matches against customerName OR customerPhone so a saved number can be searched directly
    // (e.g. from a WhatsApp contact) without needing the customer's name.
    @Query("""
        SELECT q FROM Quote q
        WHERE (:status IS NULL OR q.status = :status)
        AND (
            LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR q.customerPhone LIKE CONCAT('%', :search, '%')
        )
        ORDER BY q.updatedAt DESC
        """)
    List<Quote> search(@Param("status") QuoteStatus status, @Param("search") String search);
}