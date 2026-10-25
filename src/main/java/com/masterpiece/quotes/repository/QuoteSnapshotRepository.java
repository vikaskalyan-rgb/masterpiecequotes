package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.QuoteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuoteSnapshotRepository extends JpaRepository<QuoteSnapshot, Long> {

    List<QuoteSnapshot> findByQuoteIdOrderByCreatedAtDesc(Long quoteId);

    Optional<QuoteSnapshot> findFirstByQuoteIdOrderByCreatedAtDesc(Long quoteId);

    Optional<QuoteSnapshot> findByIdAndQuoteId(Long id, Long quoteId);

    long countByQuoteId(Long quoteId);
}
