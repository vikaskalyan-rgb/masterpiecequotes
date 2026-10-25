package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.AnalyticsDto;
import com.masterpiece.quotes.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics")
    public AnalyticsDto analytics() {
        return analyticsService.compute();
    }
}
