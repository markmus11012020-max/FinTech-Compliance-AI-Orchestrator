package com.fintech.compliance.provider;

import com.fintech.compliance.config.ComplianceProperties;
import com.fintech.compliance.dto.GigaChatDto;
import com.fintech.compliance.exception.ComplianceException;
import com.fintech.compliance.service.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Реализация LLM-провайдера для GigaChat (Сбер).
 * Документация: https://developers.sber.ru/docs/ru/gigachat/api/reference
 */
@Component
public class GigaChatProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GigaChatProvider.class);
    private static final String SYSTEM_PROMPT = YandexGptProvider.SYSTEM_PROMPT;

    private final RestTemplate restTemplate;
    private final ComplianceProperties.GigaChat config;
    private final double temperature;

    public GigaChatProvider(RestTemplate restTemplate, ComplianceProperties properties) {
        this.restTemplate = restTemplate;
        this.config = properties.getGigachat();
        this.temperature = properties.getLlm().getTemperature();
    }

    @Override
    public String name() {
        return "gigachat";
    }

    @Override
    public String complete(String maskedTransactionText) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new ComplianceException("GIGACHAT_API_KEY не задан");
        }
        GigaChatDto.ChatRequest request = new GigaChatDto.ChatRequest(
                config.getModel(),
                List.of(
                        new GigaChatDto.Msg("system", SYSTEM_PROMPT),
                        new GigaChatDto.Msg("user", maskedTransactionText)
                ),
                temperature
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        headers.add("X-Request-ID", java.util.UUID.randomUUID().toString());
        headers.add("X-Session-ID", java.util.UUID.randomUUID().toString());
        headers.add("X-Client-ID", "fintech-compliance-orchestrator");
        headers.add("RqUID", java.util.UUID.randomUUID().toString());
        // Scope не передаётся в заголовке для GigaChat API (передаётся только в OAuth-обмене).
        // Хранится в конфиге для сведения.

        HttpEntity<GigaChatDto.ChatRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<GigaChatDto.ChatResponse> response = restTemplate.postForEntity(
                    config.getUrl(), entity, GigaChatDto.ChatResponse.class);
            GigaChatDto.ChatResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                throw new ComplianceException("GigaChat вернул пустой ответ");
            }
            return body.getChoices().get(0).getMessage().getContent();
        } catch (RestClientException e) {
            log.error("Ошибка вызова GigaChat: {}", e.getMessage());
            throw new ComplianceException("GigaChat недоступен: " + e.getMessage(), e);
        }
    }
}