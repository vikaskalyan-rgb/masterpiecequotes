package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.*;
import com.masterpiece.quotes.entity.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class QuoteMapper {

    private final JsonMapper json;

    public QuoteMapper(JsonMapper json) {
        this.json = json;
    }

    public QuoteSummaryDto toSummary(Quote q) {
        return new QuoteSummaryDto(q.getId(), q.getQuoteNumber(), q.getCustomerName(), q.getCustomerPhone(),
                q.getCustomerAddress(), q.getQuoteDate(), q.getValidUntil(), q.getStatus(), q.getSubtotal(),
                q.getRoundedTotal(), q.isTotalOverridden(), q.getFirstSentAt(), q.getUpdatedAt(), q.getDeletedAt());
    }

    public QuoteDetailDto toDetail(Quote q, BusinessProfile profile, QuoteSnapshot latestShare, long shareCount) {
        List<RoomDto> rooms = q.getRooms().stream()
                .sorted(Comparator.comparing(Room::getSortOrder))
                .map(this::toRoom)
                .toList();
        List<MaterialSpecItemDto> specs = q.getMaterialSpecItems().stream()
                .sorted(Comparator.comparing(MaterialSpecItem::getSortOrder))
                .map(s -> new MaterialSpecItemDto(s.getItemLabel(), s.getDetail(), s.getBrand()))
                .toList();
        List<TermItemDto> terms = q.getTermItems().stream()
                .sorted(Comparator.comparing(TermItem::getSortOrder))
                .map(t -> new TermItemDto(t.getText()))
                .toList();
        BigDecimal suggested = Totals.suggestedTotal(q.getSubtotal(), profile.getRoundingMode(), profile.getRoundingStep());
        boolean changed = latestShare != null && !latestShare.getContentHash().equals(contentHash(q));

        return new QuoteDetailDto(q.getId(), q.getQuoteNumber(), q.getVersion(), q.getCustomerName(),
                q.getCustomerPhone(), q.getCustomerAddress(), q.getQuoteDate(), q.getValidUntil(), q.getStatus(),
                q.getSubtotal(), q.getRoundedTotal(), q.isTotalOverridden(), suggested,
                q.getAccessoriesDescription(), q.getAccessoriesAmount(), rooms, specs, terms, q.getCreatedAt(),
                q.getUpdatedAt(), q.getFirstSentAt(), latestShare != null ? latestShare.getCreatedAt() : null,
                changed, shareCount, q.getDeletedAt());
    }

    private RoomDto toRoom(Room room) {
        List<ItemDto> items = room.getItems().stream()
                .sorted(Comparator.comparing(Item::getSortOrder))
                .map(i -> new ItemDto(i.getId(), i.getDescription(), i.getLength(), i.getWidth(), i.getQuantity(),
                        i.getUnit(), i.getRate(), i.getAmount()))
                .toList();
        BigDecimal total = items.stream().map(ItemDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RoomDto(room.getId(), room.getName(), total, items);
    }

    public String toJson(Object value) {
        return json.writeValueAsString(value);
    }

    public <T> T fromJson(String value, Class<T> type) {
        return json.readValue(value, type);
    }

    // Everything the customer sees, and nothing else (no ids, status or timestamps). Numbers are
    // normalized so 7.50 and 7.5 hash the same.
    public String contentHash(Quote q) {
        var content = new HashContent(q.getCustomerName(), q.getCustomerPhone(), q.getCustomerAddress(),
                q.getQuoteDate(), q.getValidUntil(), num(q.getRoundedTotal()), q.getAccessoriesDescription(),
                num(q.getAccessoriesAmount()),
                q.getRooms().stream().sorted(Comparator.comparing(Room::getSortOrder))
                        .map(r -> new HashRoom(r.getName(), r.getItems().stream()
                                .sorted(Comparator.comparing(Item::getSortOrder))
                                .map(i -> new HashItem(i.getDescription(), i.getUnit().name(), num(i.getLength()),
                                        num(i.getWidth()), num(i.getQuantity()), num(i.getRate())))
                                .toList()))
                        .toList(),
                q.getMaterialSpecItems().stream().sorted(Comparator.comparing(MaterialSpecItem::getSortOrder))
                        .map(s -> s.getItemLabel() + "|" + s.getDetail() + "|" + s.getBrand()).toList(),
                q.getTermItems().stream().sorted(Comparator.comparing(TermItem::getSortOrder))
                        .map(TermItem::getText).toList());
        return sha256(toJson(content));
    }

    private static String num(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private static String sha256(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record HashContent(String name, String phone, String address, LocalDate date, LocalDate validUntil,
                               String total, String accessories, String accessoriesAmount, List<HashRoom> rooms,
                               List<String> specs, List<String> terms) {
    }

    private record HashRoom(String name, List<HashItem> items) {
    }

    private record HashItem(String description, String unit, String length, String width, String quantity,
                            String rate) {
    }
}
