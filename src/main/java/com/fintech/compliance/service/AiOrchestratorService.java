package com.fintech.compliance.service;

import com.fintech.compliance.dto.ComplianceReport;
import com.fintech.compliance.dto.MaskedPayload;
import com.fintech.compliance.entity.AuditLog;
import com.fintech.compliance.exception.ComplianceException;
import com.fintech.compliance.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Главный оркестратор AML-проверки.
 * Конвейер:
 *   1) Анонимизация входного текста (DataMaskingService) — 152-ФЗ.
 *   2) Вызов активного LLM-провайдера через фабрику.
 *   3) Парсинг строгого JSON-layout.
 *   4) Деанонимизация reason/полей с токенами.
 *   5) Запись аудита (без ПДн — только метаданные и токены).
 *   6) Возврат чистого финального отчёта.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private final DataMaskingService maskingService;
    private final LlmProviderFactory providerFactory;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AiOrchestratorService(DataMaskingService maskingService,
                                 LlmProviderFactory providerFactory,
                                 AuditLogRepository auditLogRepository,
                                 ObjectMapper objectMapper) {
        this.maskingService = maskingService;
        this.providerFactory = providerFactory;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public ComplianceReport runCheck(String transactionId, String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new ComplianceException("Пустой payload недопустим");
        }

        LlmProvider provider = providerFactory.active();
        log.info("Compliance-проверка [{}], провайдер={}", transactionId, provider.name());

        MaskedPayload masked = maskingService.anonymize(rawPayload);
        log.debug("Анонимизировано {} сущностей", masked.getTokenMap().size());

        String rawAnswer;
        try {
            rawAnswer = provider.complete(masked.getMaskedText());
        } catch (Exception ex) {
            log.error("LLM {} вернул ошибку: {}", provider.name(), ex.getMessage());
            persistAudit(transactionId, provider.name(), masked.getTokenMap().size(), "ERROR", null);
            throw ex;
        }

        ComplianceReport report = parseStrictJson(rawAnswer);
        String deanonymizedReason = maskingService.deanonymize(report.getReason(), masked.getTokenMap());
        report.setReason(deanonymizedReason);

        persistAudit(transactionId, provider.name(), masked.getTokenMap().size(),
                report.getRiskLevel(), report.isSuspicious());

        log.info("Compliance-проверка [{}] завершена: suspicious={}, risk={}",
                transactionId, report.isSuspicious(), report.getRiskLevel());
        return report;
    }

    /**
     * Распарсить строгий JSON-layout и поднять ошибку при несоответствии.
     * Допускается обёртка в markdown ```json ... ``` — извлекаем первый {...}.
     */
    private ComplianceReport parseStrictJson(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new ComplianceException("LLM вернул пустой ответ");
        }
        String json = extractFirstJsonObject(answer);
        try {
            JsonNode node = objectMapper.readTree(json);
            boolean suspicious = node.path("is_suspicious").asBoolean(false);
            String risk = node.path("risk_level").asText("LOW");
            String reason = node.path("reason").asText("");
            if (!risk.matches("LOW|MEDIUM|HIGH")) {
                risk = "LOW";
            }
            return new ComplianceReport(suspicious, risk, reason);
        } catch (Exception e) {
            log.error("Не удалось распарсить JSON LLM: {}", answer);
            throw new ComplianceException("LLM вернул невалидный JSON-layout", e);
        }
    }

    private String extractFirstJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new ComplianceException("В ответе LLM не найден JSON-объект");
        }
        return text.substring(start, end + 1);
    }

    private void persistAudit(String transactionId, String provider, int maskedCount,
                              String riskLevel, Boolean suspicious) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTransactionId(transactionId);
            entry.setProvider(provider);
            entry.setMaskedEntitiesCount(maskedCount);
            entry.setRiskLevel(riskLevel);
            entry.setSuspicious(suspicious);
            entry.setCreatedAt(Instant.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Аудит не должен ломать бизнес-логику.
            log.warn("Не удалось сохранить AuditLog: {}", e.getMessage());
        }
    }

    /**
     * Вспомогательный публичный метод для получения карты доступных провайдеров.
     */
    public Map<String, String> describe() {
        return Map.of(
                "active", providerFactory.active().name(),
                "available", String.join(",", providerFactory.available())
        );
    }
}