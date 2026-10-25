package com.masterpiece.quotes.repository;

import com.masterpiece.quotes.entity.Quote;
import com.masterpiece.quotes.entity.QuoteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    // "search" and "digits" must be non-null ("" matches everyone): binding a null into
    // LOWER()/CONCAT() makes the Postgres driver infer bytea and fail.
    // "digits" is the search text with everything but digits removed; blank means "don't match
    // on phone", so typing a name never matches every phone number.
    @Query("""
        SELECT q FROM Quote q
        WHERE q.deletedAt IS NULL
        AND (:status IS NULL OR q.status = :status)
        AND (
            LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            OR (:digits <> '' AND q.customerPhone LIKE CONCAT('%', :digits, '%'))
            OR (:number IS NOT NULL AND q.quoteNumber = :number)
        )
        ORDER BY q.updatedAt DESC
        """)
    List<Quote> search(@Param("status") QuoteStatus status,
                       @Param("search") String search,
                       @Param("digits") String digits,
                       @Param("number") Integer number);

    @Query("SELECT q FROM Quote q WHERE q.deletedAt IS NOT NULL ORDER BY q.deletedAt DESC")
    List<Quote> findDeleted();

    @Query("SELECT q FROM Quote q WHERE q.id = :id AND q.deletedAt IS NULL")
    Optional<Quote> findActive(@Param("id") Long id);

    Optional<Quote> findByClientRequestId(String clientRequestId);

    boolean existsByQuoteNumber(Integer quoteNumber);

    @Query("SELECT q FROM Quote q WHERE q.deletedAt IS NULL")
    List<Quote> findAllActive();

    @Query("SELECT q FROM Quote q ORDER BY q.quoteNumber")
    List<Quote> findAllForBackup();

    // Most recent quotes whose customer matches - used to auto-fill details for repeat customers.
    @Query("""
        SELECT q FROM Quote q
        WHERE q.deletedAt IS NULL
        AND (
            LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
            OR (:digits <> '' AND q.customerPhone LIKE CONCAT('%', :digits, '%'))
        )
        ORDER BY q.updatedAt DESC
        """)
    List<Quote> findCustomerMatches(@Param("search") String search,
                                    @Param("digits") String digits,
                                    Pageable pageable);

    // Used by backup restore to keep a quote's original number.
    @Modifying
    @Query(value = "UPDATE quotes SET quote_number = :number WHERE id = :id", nativeQuery = true)
    void assignQuoteNumber(@Param("id") Long id, @Param("number") Integer number);

    // After restoring quotes with their original numbers, continue numbering after the highest.
    @Query(value = "SELECT setval('quote_number_seq', (SELECT COALESCE(MAX(quote_number), 0) FROM quotes) + 1, false)",
           nativeQuery = true)
    Long resyncQuoteNumberSequence();
}
