package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.BusinessProfileDto;
import com.masterpiece.quotes.dto.DefaultsDto;
import com.masterpiece.quotes.service.BusinessProfileService;
import com.masterpiece.quotes.service.DefaultsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final DefaultsService defaultsService;
    private final BusinessProfileService profileService;

    public SettingsController(DefaultsService defaultsService, BusinessProfileService profileService) {
        this.defaultsService = defaultsService;
        this.profileService = profileService;
    }

    // Pre-fills Material Spec + Terms on every new quote.
    @GetMapping("/defaults")
    public DefaultsDto getDefaults() {
        return defaultsService.get();
    }

    @PutMapping("/defaults")
    public DefaultsDto updateDefaults(@Valid @RequestBody DefaultsDto request) {
        return defaultsService.update(request);
    }

    // Organisation details printed on the PDF, rounding rule and quote validity.
    @GetMapping("/business")
    public BusinessProfileDto getBusiness() {
        return profileService.get();
    }

    @PutMapping("/business")
    public BusinessProfileDto updateBusiness(@Valid @RequestBody BusinessProfileDto request) {
        return profileService.update(request);
    }
}
