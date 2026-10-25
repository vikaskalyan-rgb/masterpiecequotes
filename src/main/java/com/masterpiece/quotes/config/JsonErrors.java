package com.masterpiece.quotes.config;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

// Minimal JSON error writer for filters, which run before Spring MVC's message converters.
public final class JsonErrors {

    private JsonErrors() {
    }

    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"message\":\"" + safe + "\"}");
    }
}
