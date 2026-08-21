package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.DefaultsDto;
import com.masterpiece.quotes.service.DefaultsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/defaults")
public class DefaultsController {

    private final DefaultsService defaultsService;

    public DefaultsController(DefaultsService defaultsService) {
        this.defaultsService = defaultsService;
    }

    // Frontend calls this when starting a brand new quote, to pre-fill Material Spec + Terms.
    @GetMapping
    public DefaultsDto get() {
        return defaultsService.get();
    }

    // Dad edits these in a Settings screen; future quotes pick up the new defaults.
    @PutMapping
    public DefaultsDto update(@Valid @RequestBody DefaultsDto request) {
        return defaultsService.update(request);
    }
}
