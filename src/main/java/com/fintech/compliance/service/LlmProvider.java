package com.fintech.compliance.service;

/**
 * Контракт LLM-провайдера. Реализуется Mock/YandexGPT/GigaChat.
 * Стратегия (Strategy) — позволяет расширять список провайдеров без правок в оркестраторе.
 */
public interface LlmProvider {

    /**
     * Человеко-читаемое имя провайдера.
     */
    String name();

    /**
     * Проверить, может ли провайдер работать с текущей конфигурацией (имеет ли ключи).
     * Используется для graceful fallback к mock-провайдеру.
     */
    boolean canWork();

    /**
     * Отправить текст транзакции (уже анонимизированный) в LLM и получить сырой JSON-ответ.
     * Контракт: возвращается строка, содержащая JSON-объект
     * {"is_suspicious": bool, "risk_level": "LOW|MEDIUM|HIGH", "reason": "..."}.
     * Если LLM вернула обёрнутый ответ — извлечь полезный JSON наружу.
     */
    String complete(String maskedTransactionText);
}