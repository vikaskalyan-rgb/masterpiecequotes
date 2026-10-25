package com.masterpiece.quotes.controller;

import com.masterpiece.quotes.dto.ClientErrorReport;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    private static final Logger clientLog = LoggerFactory.getLogger("client-errors");

    private final JdbcTemplate jdbc;

    public SystemController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Public liveness check for UptimeRobot. Deliberately does NOT touch the database, so
    // pinging it every 5 minutes keeps Render awake without keeping Neon's compute running.
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // Public readiness check (for a Render health check): verifies the database is reachable.
    @GetMapping("/api/health/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (RuntimeException e) {
            log.error("Readiness check failed: database unreachable", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "database unavailable"));
        }
    }

    // Lets the login screen verify the password. Reaching here means it was accepted.
    @GetMapping("/api/auth/check")
    public ResponseEntity<Void> authCheck() {
        return ResponseEntity.noContent().build();
    }

    // Frontend crashes land in Render's logs, so a broken screen gets noticed.
    @PostMapping("/api/client-errors")
    public ResponseEntity<Void> clientError(@Valid @RequestBody ClientErrorReport report) {
        clientLog.warn("Frontend error [{}] at {} ({}): {}\n{}", report.release(), report.url(), report.userAgent(),
                report.message(), report.stack() == null ? "" : report.stack());
        return ResponseEntity.accepted().build();
    }
}
