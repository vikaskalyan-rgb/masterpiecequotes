package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.AnalyticsDto;
import com.masterpiece.quotes.entity.Quote;
import com.masterpiece.quotes.entity.QuoteStatus;
import com.masterpiece.quotes.repository.QuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

// Business numbers are computed here, once, so every screen agrees. Drafts are work in progress
// and never count as "quoted"; only quotes that were actually sent do.
@Service
public class AnalyticsService {

    private static final int MONTHS = 6;
    private static final int STALE_DRAFT_DAYS = 7;

    private final QuoteRepository quotes;
    private final ZoneId zone;
    private final Clock clock;

    @Autowired
    public AnalyticsService(QuoteRepository quotes, @Value("${app.timezone}") String timezone) {
        this(quotes, ZoneId.of(timezone), Clock.systemUTC());
    }

    AnalyticsService(QuoteRepository quotes, ZoneId zone, Clock clock) {
        this.quotes = quotes;
        this.zone = zone;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsDto compute() {
        List<Quote> all = quotes.findAllActive();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant staleBefore = clock.instant().minus(STALE_DRAFT_DAYS, ChronoUnit.DAYS);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (QuoteStatus s : QuoteStatus.values()) {
            statusCounts.put(s.name(), 0L);
        }

        List<Quote> sent = new ArrayList<>();
        long staleDrafts = 0;
        for (Quote q : all) {
            statusCounts.merge(q.getStatus().name(), 1L, Long::sum);
            if (q.getStatus() == QuoteStatus.DRAFT) {
                if (q.getUpdatedAt().isBefore(staleBefore)) {
                    staleDrafts++;
                }
            } else {
                sent.add(q);
            }
        }

        BigDecimal quotedValue = sum(sent);
        List<Quote> won = sent.stream().filter(q -> q.getStatus() == QuoteStatus.ACCEPTED).toList();
        long rejected = statusCounts.get(QuoteStatus.REJECTED.name());
        long decided = won.size() + rejected;
        Double conversion = decided == 0 ? null : (double) won.size() / decided;
        BigDecimal average = sent.isEmpty() ? BigDecimal.ZERO
                : quotedValue.divide(BigDecimal.valueOf(sent.size()), 0, RoundingMode.HALF_UP);

        return new AnalyticsDto(statusCounts, quotedValue, sent.size(), sum(won), won.size(), conversion, average,
                months(sent, today), topCustomers(sent), staleDrafts);
    }

    private List<AnalyticsDto.Month> months(List<Quote> sent, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        Map<YearMonth, List<Quote>> byMonth = new HashMap<>();
        for (Quote q : sent) {
            byMonth.computeIfAbsent(YearMonth.from(q.getQuoteDate()), k -> new ArrayList<>()).add(q);
        }
        List<AnalyticsDto.Month> result = new ArrayList<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            List<Quote> inMonth = byMonth.getOrDefault(m, List.of());
            List<Quote> wonInMonth = inMonth.stream().filter(q -> q.getStatus() == QuoteStatus.ACCEPTED).toList();
            result.add(new AnalyticsDto.Month(m.toString(), sum(inMonth), sum(wonInMonth), inMonth.size()));
        }
        return result;
    }

    private List<AnalyticsDto.Customer> topCustomers(List<Quote> sent) {
        Map<String, List<Quote>> byPhone = new HashMap<>();
        for (Quote q : sent) {
            byPhone.computeIfAbsent(q.getCustomerPhone(), k -> new ArrayList<>()).add(q);
        }
        return byPhone.entrySet().stream()
                .map(e -> {
                    Quote latest = e.getValue().stream().max(Comparator.comparing(Quote::getUpdatedAt)).orElseThrow();
                    return new AnalyticsDto.Customer(latest.getCustomerName(), e.getKey(), sum(e.getValue()), e.getValue().size());
                })
                .sorted(Comparator.comparing(AnalyticsDto.Customer::value).reversed())
                .limit(5)
                .toList();
    }

    private static BigDecimal sum(List<Quote> list) {
        return list.stream().map(q -> Totals.nz(q.getRoundedTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
