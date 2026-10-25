package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.*;
import com.masterpiece.quotes.entity.*;
import com.masterpiece.quotes.exception.ConflictException;
import com.masterpiece.quotes.exception.InvalidInputException;
import com.masterpiece.quotes.exception.ResourceNotFoundException;
import com.masterpiece.quotes.repository.ItemRepository;
import com.masterpiece.quotes.repository.QuoteRepository;
import com.masterpiece.quotes.repository.QuoteSnapshotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class QuoteService {

    private final QuoteRepository quotes;
    private final QuoteSnapshotRepository snapshots;
    private final ItemRepository items;
    private final BusinessProfileService profiles;
    private final QuoteMapper mapper;

    public QuoteService(QuoteRepository quotes, QuoteSnapshotRepository snapshots, ItemRepository items,
                        BusinessProfileService profiles, QuoteMapper mapper) {
        this.quotes = quotes;
        this.snapshots = snapshots;
        this.items = items;
        this.profiles = profiles;
        this.mapper = mapper;
    }

    // ---------- queries ----------

    @Transactional(readOnly = true)
    public List<QuoteSummaryDto> list(QuoteStatus status, String search) {
        String text = search == null ? "" : search.trim();
        String digits = PhoneNumbers.digitsOnly(text);
        Integer number = parseQuoteNumber(text);
        return quotes.search(status, escapeLike(text), digits.length() >= 3 ? digits : "", number)
                .stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteSummaryDto> listDeleted() {
        return quotes.findDeleted().stream().map(mapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public QuoteDetailDto getById(Long id) {
        return detail(findActive(id));
    }

    // ---------- writes ----------

    @Transactional
    public QuoteDetailDto create(QuoteRequest request) {
        if (request.clientRequestId() != null && !request.clientRequestId().isBlank()) {
            Optional<Quote> existing = quotes.findByClientRequestId(request.clientRequestId());
            if (existing.isPresent()) {
                return detail(existing.get());
            }
        }
        Quote quote = new Quote();
        quote.setClientRequestId(blankToNull(request.clientRequestId()));
        apply(quote, request);
        Quote saved = quotes.saveAndFlush(quote);
        return detail(saved);
    }

    @Transactional
    public QuoteDetailDto update(Long id, QuoteRequest request) {
        Quote quote = findActive(id);
        if (request.version() == null) {
            throw new InvalidInputException("version", "is required when updating a quote");
        }
        if (!request.version().equals(quote.getVersion())) {
            throw new ConflictException("This quote was changed on another device. Reload it to see the latest version.",
                    quote.getVersion());
        }
        apply(quote, request);
        quote.touch(); // also guarantees the version increments when only rooms/items changed
        return detail(quotes.saveAndFlush(quote));
    }

    @Transactional
    public QuoteDetailDto updateStatus(Long id, QuoteStatus status) {
        Quote quote = findActive(id);
        quote.setStatus(status);
        if (status != QuoteStatus.DRAFT && quote.getFirstSentAt() == null) {
            quote.setFirstSentAt(Instant.now());
        }
        quote.touch();
        return detail(quotes.saveAndFlush(quote));
    }

    // Records exactly what is being sent to the customer. Sharing the same content twice reuses
    // the existing version; sharing after an edit creates a new one.
    @Transactional
    public ShareResultDto share(Long id) {
        Quote quote = findActive(id);
        BusinessProfile profile = profiles.current();
        String hash = mapper.contentHash(quote);
        Optional<QuoteSnapshot> latest = snapshots.findFirstByQuoteIdOrderByCreatedAtDesc(id);

        QuoteSnapshot snapshot;
        boolean created;
        if (latest.isPresent() && latest.get().getContentHash().equals(hash)) {
            snapshot = latest.get();
            created = false;
        } else {
            snapshot = new QuoteSnapshot();
            snapshot.setQuote(quote);
            snapshot.setContentHash(hash);
            snapshot.setRoundedTotal(quote.getRoundedTotal());
            QuoteDetailDto asShared = mapper.toDetail(quote, profile, null, 0);
            snapshot.setPayload(mapper.toJson(new SnapshotPayload(asShared, BusinessProfileService.toDto(profile))));
            snapshot = snapshots.save(snapshot);
            created = true;
        }

        // Only touch the quote when something about it changes, so re-sharing an unchanged quote
        // doesn't bump its version (which would wrongly conflict with an open copy elsewhere).
        boolean changed = created;
        if (quote.getStatus() == QuoteStatus.DRAFT) {
            quote.setStatus(QuoteStatus.SENT);
            changed = true;
        }
        if (quote.getFirstSentAt() == null) {
            quote.setFirstSentAt(Instant.now());
            changed = true;
        }
        if (changed) {
            quote.touch();
            quotes.saveAndFlush(quote);
        }

        var summary = new SnapshotSummaryDto(snapshot.getId(), snapshot.getCreatedAt(), snapshot.getRoundedTotal());
        return new ShareResultDto(summary, created, detail(quote));
    }

    @Transactional(readOnly = true)
    public List<SnapshotSummaryDto> listSnapshots(Long quoteId) {
        findAny(quoteId);
        return snapshots.findByQuoteIdOrderByCreatedAtDesc(quoteId).stream()
                .map(s -> new SnapshotSummaryDto(s.getId(), s.getCreatedAt(), s.getRoundedTotal()))
                .toList();
    }

    @Transactional(readOnly = true)
    public String snapshotPayload(Long quoteId, Long snapshotId) {
        return snapshots.findByIdAndQuoteId(snapshotId, quoteId)
                .map(QuoteSnapshot::getPayload)
                .orElseThrow(() -> new ResourceNotFoundException("Shared version not found"));
    }

    @Transactional
    public void delete(Long id) {
        Quote quote = findActive(id);
        quote.setDeletedAt(Instant.now());
        quote.touch();
        quotes.save(quote);
    }

    @Transactional
    public QuoteDetailDto restore(Long id) {
        Quote quote = findAny(id);
        quote.setDeletedAt(null);
        quote.touch();
        return detail(quotes.saveAndFlush(quote));
    }

    // ---------- autocomplete ----------

    @Transactional(readOnly = true)
    public List<ItemSuggestionDto> itemSuggestions(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        Map<String, ItemSuggestionDto> unique = new LinkedHashMap<>();
        for (Object[] row : items.findSuggestions(escapeLike(prefix.trim()), PageRequest.of(0, 30))) {
            String description = (String) row[0];
            unique.putIfAbsent(description.toLowerCase(Locale.ROOT), new ItemSuggestionDto(description, (ItemUnit) row[1]));
            if (unique.size() == 8) {
                break;
            }
        }
        return List.copyOf(unique.values());
    }

    @Transactional(readOnly = true)
    public List<CustomerSuggestionDto> customerSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String text = query.trim();
        String digits = PhoneNumbers.digitsOnly(text);
        Map<String, CustomerSuggestionDto> byPhone = new LinkedHashMap<>();
        for (Quote q : quotes.findCustomerMatches(escapeLike(text), digits.length() >= 3 ? digits : "", PageRequest.of(0, 40))) {
            byPhone.putIfAbsent(q.getCustomerPhone(),
                    new CustomerSuggestionDto(q.getCustomerName(), q.getCustomerPhone(), q.getCustomerAddress()));
            if (byPhone.size() == 6) {
                break;
            }
        }
        return List.copyOf(byPhone.values());
    }

    // ---------- internals ----------

    Quote findActive(Long id) {
        return quotes.findActive(id).orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
    }

    private Quote findAny(Long id) {
        return quotes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
    }

    QuoteDetailDto detail(Quote quote) {
        Optional<QuoteSnapshot> latest = quote.getId() == null ? Optional.empty()
                : snapshots.findFirstByQuoteIdOrderByCreatedAtDesc(quote.getId());
        long count = quote.getId() == null ? 0 : snapshots.countByQuoteId(quote.getId());
        return mapper.toDetail(quote, profiles.current(), latest.orElse(null), count);
    }

    // Copies the request onto the entity and recomputes every derived number.
    void apply(Quote quote, QuoteRequest r) {
        BusinessProfile profile = profiles.current();

        quote.setCustomerName(r.customerName().trim());
        quote.setCustomerPhone(PhoneNumbers.normalize(r.customerPhone()));
        quote.setCustomerAddress(blankToNull(r.customerAddress()));
        quote.setQuoteDate(r.quoteDate());
        LocalDate validUntil = r.validUntil() != null ? r.validUntil() : r.quoteDate().plusDays(profile.getValidityDays());
        if (validUntil.isBefore(r.quoteDate())) {
            throw new InvalidInputException("validUntil", "can't be before the quote date");
        }
        quote.setValidUntil(validUntil);
        quote.setAccessoriesDescription(blankToNull(r.accessoriesDescription()));
        quote.setAccessoriesAmount(Totals.money(r.accessoriesAmount()));

        rebuildChildren(quote, r);

        BigDecimalSum sum = new BigDecimalSum();
        quote.getRooms().forEach(room -> room.getItems().forEach(item -> sum.add(item.getAmount())));
        quote.setSubtotal(Totals.money(sum.value().add(quote.getAccessoriesAmount())));

        quote.setTotalOverridden(r.isTotalOverridden());
        if (r.isTotalOverridden()) {
            if (r.roundedTotal() == null) {
                throw new InvalidInputException("roundedTotal", "is required when the total is set by hand");
            }
            quote.setRoundedTotal(Totals.money(r.roundedTotal()));
        } else {
            quote.setRoundedTotal(Totals.suggestedTotal(quote.getSubtotal(), profile.getRoundingMode(), profile.getRoundingStep()));
        }
    }

    // Replace-all strategy: the builder always sends the whole quote, and saves are explicit
    // (protected by the version check), so there is no partial-update merging to get wrong.
    private void rebuildChildren(Quote quote, QuoteRequest r) {
        quote.getRooms().clear();
        int roomOrder = 0;
        for (RoomRequest roomRequest : r.roomsOrEmpty()) {
            Room room = new Room();
            room.setQuote(quote);
            room.setName(roomRequest.name().trim());
            room.setSortOrder(roomOrder++);
            int itemOrder = 0;
            for (ItemRequest ir : roomRequest.itemsOrEmpty()) {
                Item item = new Item();
                item.setRoom(room);
                item.setDescription(ir.description().trim());
                item.setUnit(ir.unit());
                // Dimensions only count as a pair; a lone length or width is ignored.
                boolean dims = ir.unit() == ItemUnit.SQFT && ir.length() != null && ir.width() != null;
                item.setLength(dims ? ir.length() : null);
                item.setWidth(dims ? ir.width() : null);
                item.setQuantity(Totals.quantity(ir.unit(), item.getLength(), item.getWidth(), ir.quantity(), ir.isAreaAdjusted()));
                item.setRate(Totals.money(ir.rate()));
                item.setAmount(Totals.amount(item.getQuantity(), item.getRate()));
                item.setSortOrder(itemOrder++);
                room.getItems().add(item);
            }
            quote.getRooms().add(room);
        }

        quote.getMaterialSpecItems().clear();
        int specOrder = 0;
        for (MaterialSpecItemDto s : r.specsOrEmpty()) {
            MaterialSpecItem spec = new MaterialSpecItem();
            spec.setQuote(quote);
            spec.setItemLabel(s.itemLabel().trim());
            spec.setDetail(blankToNull(s.detail()));
            spec.setBrand(blankToNull(s.brand()));
            spec.setSortOrder(specOrder++);
            quote.getMaterialSpecItems().add(spec);
        }

        quote.getTermItems().clear();
        int termOrder = 0;
        for (TermItemDto t : r.termsOrEmpty()) {
            TermItem term = new TermItem();
            term.setQuote(quote);
            term.setText(t.text().trim());
            term.setSortOrder(termOrder++);
            quote.getTermItems().add(term);
        }
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // Accepts "42", "MP-42", "mp-0042", "#42".
    static Integer parseQuoteNumber(String s) {
        String t = s.trim().toUpperCase(Locale.ROOT).replaceFirst("^(MP-?|#)", "");
        if (t.isEmpty() || t.length() > 9 || !t.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.parseInt(t);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static final class BigDecimalSum {
        private BigDecimal value = BigDecimal.ZERO;

        void add(BigDecimal v) {
            value = value.add(Totals.nz(v));
        }

        BigDecimal value() {
            return value;
        }
    }
}
