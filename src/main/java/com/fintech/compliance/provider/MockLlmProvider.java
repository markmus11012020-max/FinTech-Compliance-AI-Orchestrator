package com.fintech.compliance.provider;

import com.fintech.compliance.service.LlmProvider;
import org.springframework.stereotype.Component;

/**
 * Безопасный мок-провайдер. Не делает внешних вызовов.
 * Используется для разработки, тестов и CI.
 */
@Component
public class MockLlmProvider implements LlmProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean canWork() {
        return true; // Mock всегда работает
    }

    @Override
    public String complete(String maskedTransactionText) {
        boolean suspicious = maskedTransactionText != null
                && maskedTransactionText.toLowerCase().contains("[token_");
        String level = suspicious ? "MEDIUM" : "LOW";
        String reason = suspicious
                ? "Обнаружены токенизированные сущности, требуется ручная верификация."
                : "Транзакция не содержит признаков подозрительной активности.";
        return String.format(
                "{\"is_suspicious\":%s,\"risk_level\":\"%s\",\"reason\":\"%s\"}",
                suspicious, level, reason);
    }
}