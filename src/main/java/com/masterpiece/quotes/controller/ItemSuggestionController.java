package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.repository.ItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ItemSuggestionController {

    private final ItemRepository itemRepository;

    public ItemSuggestionController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Autocomplete for item descriptions, based on what he's typed before. Rate is NEVER suggested here.
    @GetMapping("/api/items/suggestions")
    public List<String> suggestions(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return itemRepository.findSuggestions(q, PageRequest.of(0, 8));
    }
}
