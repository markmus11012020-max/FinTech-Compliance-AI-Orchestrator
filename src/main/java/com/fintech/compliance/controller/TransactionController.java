package com.fintech.compliance.controller;

import com.fintech.compliance.dto.CheckRequest;
import com.fintech.compliance.dto.ComplianceReport;
import com.fintech.compliance.exception.ComplianceException;
import com.fintech.compliance.service.AiOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-контроллер AML compliance-проверок.
 *
 * Эндпоинты:
 *  POST /api/v1/compliance/check — основной пайплайн Anonymize -> LLM -> Deanonymize.
 *  GET  /api/v1/compliance/info   — информация об активном провайдере.
 */
@RestController
@RequestMapping(value = "/api/v1/compliance", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private final AiOrchestratorService orchestrator;

    public TransactionController(AiOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Полный пайплайн проверки:
     * 1) Анонимизация.
     * 2) Запрос к российскому LLM.
     * 3) Деанонимизация.
     */
    @PostMapping(value = "/check", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComplianceReport> check(@RequestBody CheckRequest request) {
        if (request == null || request.getPayload() == null || request.getPayload().isBlank()) {
            throw new ComplianceException("Поле 'payload' обязательно и не должно быть пустым");
        }
        ComplianceReport report = orchestrator.runCheck(
                request.getTransactionId(),
                request.getPayload()
        );
        return ResponseEntity.ok(report);
    }

    /**
     * Альтернативный эндпоинт, принимающий "сырой" текст транзакции как строку.
     * Удобен для интеграции с фронтом/curl.
     */
    @PostMapping(value = "/check-raw", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ComplianceReport> checkRaw(@RequestBody String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ComplianceException("Тело запроса не должно быть пустым");
        }
        return ResponseEntity.ok(orchestrator.runCheck(null, rawText));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(orchestrator.describe());
    }
}