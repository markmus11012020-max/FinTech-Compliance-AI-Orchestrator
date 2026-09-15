package com.fintech.compliance.service;

import com.fintech.compliance.config.ComplianceProperties;
import com.fintech.compliance.provider.GigaChatProvider;
import com.fintech.compliance.provider.MockLlmProvider;
import com.fintech.compliance.provider.YandexGptProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фабрика LLM-провайдеров. Возвращает активного провайдера по конфигу.
 * Расширение списка провайдеров делается через DI без изменения оркестратора.
 * Если выбранный провайдер не может работать (нет ключей), автоматически используется mock.
 */
@Component
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);

    private final ComplianceProperties properties;
    private final Map<String, LlmProvider> providers;
    private final MockLlmProvider mockProvider;

    public LlmProviderFactory(ComplianceProperties properties,
                              MockLlmProvider mock,
                              YandexGptProvider yandex,
                              GigaChatProvider giga) {
        this.properties = properties;
        this.mockProvider = mock;
        Map<String, LlmProvider> map = new HashMap<>();
        map.put(mock.name(), mock);
        map.put(yandex.name(), yandex);
        map.put(giga.name(), giga);
        this.providers = Map.copyOf(map);
    }

    public LlmProvider active() {
        String requested = properties.getLlm().getProvider();
        LlmProvider provider = providers.get(requested.toLowerCase());
        if (provider == null) {
            log.warn("Неизвестный LLM_PROVIDER: {}. Используется mock.", requested);
            return mockProvider;
        }
        // Проверяем, может ли провайдер работать с текущей конфигурацией
        if (!provider.canWork()) {
            log.warn("Провайдер {} не может работать (отсутствуют ключи). Используется mock.", requested);
            return mockProvider;
        }
        return provider;
    }

    public List<String> available() {
        return List.copyOf(providers.keySet());
    }
}