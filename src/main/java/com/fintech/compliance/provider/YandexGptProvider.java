package com.fintech.compliance.provider;

import com.fintech.compliance.config.ComplianceProperties;
import com.fintech.compliance.dto.YandexGptDto;
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
 * Реализация LLM-провайдера для YandexGPT (Yandex Cloud Foundation Models).
 * Документация: https://cloud.yandex.ru/docs/yandexgpt/api-ref/
 * Передаются только токенизированные данные, реальные ПДн не покидают периметр.
 */
@Component
public class YandexGptProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(YandexGptProvider.class);
    public static final String SYSTEM_PROMPT = buildSystemPrompt();

    private final RestTemplate restTemplate;
    private final ComplianceProperties.YandexGpt config;
    private final double temperature;

    public YandexGptProvider(RestTemplate restTemplate, ComplianceProperties properties) {
        this.restTemplate = restTemplate;
        this.config = properties.getYandexgpt();
        this.temperature = properties.getLlm().getTemperature();
    }

    @Override
    public String name() {
        return "yandexgpt";
    }

    @Override
    public String complete(String maskedTransactionText) {
        validateCredentials();
        String modelUri = String.format("gpt://%s/%s", config.getFolderId(), config.getModel());

        YandexGptDto.CompletionOptions options = new YandexGptDto.CompletionOptions(temperature, 2000);
        List<YandexGptDto.Message> messages = List.of(
                new YandexGptDto.Message("system", SYSTEM_PROMPT),
                new YandexGptDto.Message("user", maskedTransactionText)
        );
        YandexGptDto.CompletionRequest request = new YandexGptDto.CompletionRequest(modelUri, options, messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        headers.add("x-folder-id", config.getFolderId());

        HttpEntity<YandexGptDto.CompletionRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<YandexGptDto.CompletionResponse> response = restTemplate.postForEntity(
                    config.getUrl(), entity, YandexGptDto.CompletionResponse.class);
            YandexGptDto.CompletionResponse body = response.getBody();
            if (body == null || body.getResult() == null
                    || body.getResult().getAlternatives() == null
                    || body.getResult().getAlternatives().isEmpty()) {
                throw new ComplianceException("YandexGPT вернул пустой ответ");
            }
            return body.getResult().getAlternatives().get(0).getMessage().getText();
        } catch (RestClientException e) {
            log.error("Ошибка вызова YandexGPT: {}", e.getMessage());
            throw new ComplianceException("YandexGPT недоступен: " + e.getMessage(), e);
        }
    }

    private void validateCredentials() {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new ComplianceException("YANDEXGPT_API_KEY не задан");
        }
        if (config.getFolderId() == null || config.getFolderId().isBlank()) {
            throw new ComplianceException("YANDEXGPT_FOLDER_ID не задан");
        }
    }

    private static String buildSystemPrompt() {
        return "Ты — AML-compliance агент, действующий по правилам ЦБ РФ (115-ФЗ, 152-ФЗ). "
                + "Тебе передаются уже анонимизированные данные транзакций (имена, телефоны, "
                + "паспорта, карты заменены на токены вида [TOKEN_N]). "
                + "Проанализируй описание операции и верни СТРОГО один JSON-объект без пояснений, "
                + "без markdown-разметки, со схемой: "
                + "{\"is_suspicious\": boolean, \"risk_level\": \"LOW|MEDIUM|HIGH\", "
                + "\"reason\": \"краткое обоснование на русском\"}. Никакого другого текста.";
    }
}