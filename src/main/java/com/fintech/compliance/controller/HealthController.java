package com.fintech.compliance.controller;

import com.fintech.compliance.service.LlmProviderFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Контроллер состояния сервиса (liveness/readiness).
 */
@RestController
@RequestMapping(value = "/api/v1/health", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private final LlmProviderFactory providerFactory;

    public HealthController(LlmProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "fintech-compliance-orchestrator");
        body.put("timestamp", Instant.now().toString());
        body.put("provider", providerFactory.active().name());
        body.put("available_providers", providerFactory.available());
        return ResponseEntity.ok(body);
    }
}