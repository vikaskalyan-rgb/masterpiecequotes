package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.*;
import com.masterpiece.quotes.entity.*;
import com.masterpiece.quotes.exception.InvalidInputException;
import com.masterpiece.quotes.repository.QuoteRepository;
import com.masterpiece.quotes.repository.QuoteSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

// A complete export in one request (instead of the frontend fetching every quote separately),
// and an additive restore: quotes whose number already exists are skipped, never overwritten.
@Service
public class BackupService {

    private final QuoteRepository quotes;
    private final QuoteSnapshotRepository snapshots;
    private final QuoteService quoteService;
    private final DefaultsService defaults;
    private final BusinessProfileService profiles;

    public BackupService(QuoteRepository quotes, QuoteSnapshotRepository snapshots, QuoteService quoteService,
                         DefaultsService defaults, BusinessProfileService profiles) {
        this.quotes = quotes;
        this.snapshots = snapshots;
        this.quoteService = quoteService;
        this.defaults = defaults;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public BackupDto export() {
        List<Quote> all = quotes.findAllForBackup();
        List<QuoteDetailDto> details = all.stream().map(quoteService::detail).toList();
        List<BackupDto.SnapshotBackup> shared = new ArrayList<>();
        for (Quote q : all) {
            for (QuoteSnapshot s : snapshots.findByQuoteIdOrderByCreatedAtDesc(q.getId())) {
                shared.add(new BackupDto.SnapshotBackup(q.getQuoteNumber(), s.getCreatedAt(), s.getContentHash(),
                        s.getRoundedTotal(), s.getPayload()));
            }
        }
        return new BackupDto(BackupDto.FORMAT, BackupDto.FORMAT_VERSION, Instant.now(), profiles.get(),
                defaults.get(), details, shared);
    }

    @Transactional
    public RestoreResultDto restore(BackupDto backup, boolean includeSettings) {
        if (backup == null || !BackupDto.FORMAT.equals(backup.format()) || backup.quotes() == null) {
            throw new InvalidInputException("file", "This isn't a Masterpiece Quotes backup file");
        }
        if (backup.formatVersion() != null && backup.formatVersion() > BackupDto.FORMAT_VERSION) {
            throw new InvalidInputException("file", "This backup was made by a newer version of the app");
        }

        Map<Integer, Quote> restoredByNumber = new HashMap<>();
        int skipped = 0;
        for (QuoteDetailDto d : backup.quotes()) {
            if (d.quoteNumber() == null || quotes.existsByQuoteNumber(d.quoteNumber())) {
                skipped++;
                continue;
            }
            Quote q = new Quote();
            quoteService.apply(q, toRequest(d));
            // Keep what was actually agreed: a restored total is preserved exactly as backed up.
            q.setTotalOverridden(d.totalOverridden());
            q.setRoundedTotal(Totals.money(d.roundedTotal()));
            q.setValidUntil(d.validUntil());
            q.setStatus(d.status() != null ? d.status() : QuoteStatus.DRAFT);
            q.setFirstSentAt(d.firstSentAt());
            q.setDeletedAt(d.deletedAt());
            if (d.createdAt() != null) {
                q.setCreatedAt(d.createdAt());
            }
            Quote saved = quotes.saveAndFlush(q);
            quotes.assignQuoteNumber(saved.getId(), d.quoteNumber());
            restoredByNumber.put(d.quoteNumber(), saved);
        }
        quotes.resyncQuoteNumberSequence();

        int snapshotCount = 0;
        if (backup.snapshots() != null) {
            for (BackupDto.SnapshotBackup s : backup.snapshots()) {
                Quote owner = restoredByNumber.get(s.quoteNumber());
                if (owner == null) {
                    continue;
                }
                QuoteSnapshot snap = new QuoteSnapshot();
                snap.setQuote(owner);
                snap.setCreatedAt(s.createdAt());
                snap.setContentHash(s.contentHash());
                snap.setRoundedTotal(s.roundedTotal());
                snap.setPayload(s.payload());
                snapshots.save(snap);
                snapshotCount++;
            }
        }

        boolean settings = false;
        if (includeSettings) {
            if (backup.defaults() != null) {
                defaults.update(backup.defaults());
            }
            if (backup.business() != null) {
                profiles.update(backup.business());
            }
            settings = true;
        }
        return new RestoreResultDto(restoredByNumber.size(), skipped, snapshotCount, settings);
    }

    private static QuoteRequest toRequest(QuoteDetailDto d) {
        List<RoomRequest> rooms = d.rooms() == null ? List.of() : d.rooms().stream()
                .map(r -> new RoomRequest(r.name(), r.items() == null ? List.of() : r.items().stream()
                        .map(i -> new ItemRequest(i.description(), i.length(), i.width(), i.quantity(), i.unit(), i.rate(), Boolean.TRUE))
                        .toList()))
                .toList();
        return new QuoteRequest(d.customerName(), d.customerPhone(), d.customerAddress(), d.quoteDate(),
                d.validUntil(), d.roundedTotal(), d.totalOverridden(), d.accessoriesDescription(),
                d.accessoriesAmount(), rooms, d.materialSpecItems(), d.termItems(), null, null);
    }
}
