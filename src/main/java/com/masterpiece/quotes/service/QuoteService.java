package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.*;
import com.masterpiece.quotes.entity.*;
import com.masterpiece.quotes.exception.ResourceNotFoundException;
import com.masterpiece.quotes.repository.QuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Transactional(readOnly = true)
    public List<QuoteSummaryDto> list(QuoteStatus status, String search) {
        String safeSearch = (search == null) ? "" : search.trim();
        return quoteRepository.search(status, safeSearch)
                .stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuoteDetailDto getById(Long id) {
        Quote quote = findOrThrow(id);
        return toDetailDto(quote);
    }

    @Transactional
    public QuoteDetailDto create(QuoteRequest request) {
        Quote quote = new Quote();
        applyScalarFields(quote, request);
        if (quote.getStatus() == null) {
            quote.setStatus(QuoteStatus.DRAFT);
        }
        rebuildChildren(quote, request);
        recomputeTotals(quote);
        Quote saved = quoteRepository.save(quote);
        return toDetailDto(saved);
    }

    @Transactional
    public QuoteDetailDto update(Long id, QuoteRequest request) {
        Quote quote = findOrThrow(id);
        applyScalarFields(quote, request);
        rebuildChildren(quote, request);
        recomputeTotals(quote);
        Quote saved = quoteRepository.save(quote);
        return toDetailDto(saved);
    }

    @Transactional
    public QuoteDetailDto updateStatus(Long id, QuoteStatus status) {
        Quote quote = findOrThrow(id);
        quote.setStatus(status);
        Quote saved = quoteRepository.save(quote);
        return toDetailDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!quoteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quote not found: " + id);
        }
        quoteRepository.deleteById(id);
    }

    // ---------- internal helpers ----------

    private Quote findOrThrow(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found: " + id));
    }

    private void applyScalarFields(Quote quote, QuoteRequest request) {
        quote.setCustomerName(request.getCustomerName());
        quote.setCustomerPhone(request.getCustomerPhone());
        quote.setCustomerAddress(request.getCustomerAddress());
        quote.setQuoteDate(request.getQuoteDate());
        if (request.getStatus() != null) {
            quote.setStatus(request.getStatus());
        }
        quote.setAccessoriesDescription(request.getAccessoriesDescription());
        quote.setAccessoriesAmount(nz(request.getAccessoriesAmount()));
        quote.setRoundedTotal(nz(request.getRoundedTotal()));
    }

    // Replace-all-children strategy: simplest and safest for a single-user autosave builder screen.
    private void rebuildChildren(Quote quote, QuoteRequest request) {
        quote.getRooms().clear();
        int roomOrder = 0;
        for (RoomDto roomDto : request.getRooms()) {
            Room room = new Room();
            room.setQuote(quote);
            room.setName(roomDto.getName());
            room.setSortOrder(roomDto.getSortOrder() != null ? roomDto.getSortOrder() : roomOrder++);

            int itemOrder = 0;
            for (ItemDto itemDto : roomDto.getItems()) {
                Item item = new Item();
                item.setRoom(room);
                item.setDescription(itemDto.getDescription());
                item.setLength(itemDto.getLength());
                item.setWidth(itemDto.getWidth());
                item.setQuantity(nz(itemDto.getQuantity()));
                item.setUnit(itemDto.getUnit() != null ? itemDto.getUnit() : ItemUnit.SQFT);
                item.setRate(nz(itemDto.getRate()));
                // Amount always recomputed server-side, never trusted from client.
                item.setAmount(item.getQuantity().multiply(item.getRate()).setScale(2, RoundingMode.HALF_UP));
                item.setSortOrder(itemDto.getSortOrder() != null ? itemDto.getSortOrder() : itemOrder++);
                room.getItems().add(item);
            }
            quote.getRooms().add(room);
        }

        quote.getMaterialSpecItems().clear();
        int specOrder = 0;
        for (MaterialSpecItemDto specDto : request.getMaterialSpecItems()) {
            MaterialSpecItem spec = new MaterialSpecItem();
            spec.setQuote(quote);
            spec.setItemLabel(specDto.getItemLabel());
            spec.setDetail(specDto.getDetail());
            spec.setBrand(specDto.getBrand());
            spec.setSortOrder(specDto.getSortOrder() != null ? specDto.getSortOrder() : specOrder++);
            quote.getMaterialSpecItems().add(spec);
        }

        quote.getTermItems().clear();
        int termOrder = 0;
        for (TermItemDto termDto : request.getTermItems()) {
            TermItem term = new TermItem();
            term.setQuote(quote);
            term.setText(termDto.getText());
            term.setSortOrder(termDto.getSortOrder() != null ? termDto.getSortOrder() : termOrder++);
            quote.getTermItems().add(term);
        }
    }

    private void recomputeTotals(Quote quote) {
        BigDecimal itemsSum = quote.getRooms().stream()
                .flatMap(r -> r.getItems().stream())
                .map(Item::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotal = itemsSum.add(nz(quote.getAccessoriesAmount()));
        quote.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));

        // If the client didn't supply a rounded total (e.g. brand new quote), default it to the subtotal.
        if (quote.getRoundedTotal() == null || quote.getRoundedTotal().compareTo(BigDecimal.ZERO) == 0) {
            quote.setRoundedTotal(quote.getSubtotal());
        }
    }

    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ---------- DTO mapping ----------

    private QuoteSummaryDto toSummaryDto(Quote q) {
        QuoteSummaryDto dto = new QuoteSummaryDto();
        dto.setId(q.getId());
        dto.setCustomerName(q.getCustomerName());
        dto.setCustomerPhone(q.getCustomerPhone());
        dto.setCustomerAddress(q.getCustomerAddress());
        dto.setQuoteDate(q.getQuoteDate());
        dto.setStatus(q.getStatus());
        dto.setRoundedTotal(q.getRoundedTotal());
        dto.setUpdatedAt(q.getUpdatedAt());
        return dto;
    }

    private QuoteDetailDto toDetailDto(Quote q) {
        QuoteDetailDto dto = new QuoteDetailDto();
        dto.setId(q.getId());
        dto.setCustomerName(q.getCustomerName());
        dto.setCustomerPhone(q.getCustomerPhone());
        dto.setCustomerAddress(q.getCustomerAddress());
        dto.setQuoteDate(q.getQuoteDate());
        dto.setStatus(q.getStatus());
        dto.setSubtotal(q.getSubtotal());
        dto.setRoundedTotal(q.getRoundedTotal());
        dto.setAccessoriesDescription(q.getAccessoriesDescription());
        dto.setAccessoriesAmount(q.getAccessoriesAmount());
        dto.setCreatedAt(q.getCreatedAt());
        dto.setUpdatedAt(q.getUpdatedAt());

        List<RoomDto> roomDtos = new ArrayList<>();
        for (Room room : q.getRooms().stream()
                .sorted(Comparator.comparing(Room::getSortOrder)).toList()) {
            RoomDto roomDto = new RoomDto();
            roomDto.setId(room.getId());
            roomDto.setName(room.getName());
            roomDto.setSortOrder(room.getSortOrder());

            List<ItemDto> itemDtos = new ArrayList<>();
            for (Item item : room.getItems().stream()
                    .sorted(Comparator.comparing(Item::getSortOrder)).toList()) {
                ItemDto itemDto = new ItemDto();
                itemDto.setId(item.getId());
                itemDto.setDescription(item.getDescription());
                itemDto.setLength(item.getLength());
                itemDto.setWidth(item.getWidth());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnit(item.getUnit());
                itemDto.setRate(item.getRate());
                itemDto.setAmount(item.getAmount());
                itemDto.setSortOrder(item.getSortOrder());
                itemDtos.add(itemDto);
            }
            roomDto.setItems(itemDtos);
            roomDtos.add(roomDto);
        }
        dto.setRooms(roomDtos);

        List<MaterialSpecItemDto> specDtos = new ArrayList<>();
        for (MaterialSpecItem spec : q.getMaterialSpecItems().stream()
                .sorted(Comparator.comparing(MaterialSpecItem::getSortOrder)).toList()) {
            MaterialSpecItemDto specDto = new MaterialSpecItemDto();
            specDto.setId(spec.getId());
            specDto.setItemLabel(spec.getItemLabel());
            specDto.setDetail(spec.getDetail());
            specDto.setBrand(spec.getBrand());
            specDto.setSortOrder(spec.getSortOrder());
            specDtos.add(specDto);
        }
        dto.setMaterialSpecItems(specDtos);

        List<TermItemDto> termDtos = new ArrayList<>();
        for (TermItem term : q.getTermItems().stream()
                .sorted(Comparator.comparing(TermItem::getSortOrder)).toList()) {
            TermItemDto termDto = new TermItemDto();
            termDto.setId(term.getId());
            termDto.setText(term.getText());
            termDto.setSortOrder(term.getSortOrder());
            termDtos.add(termDto);
        }
        dto.setTermItems(termDtos);

        return dto;
    }
}