package com.fintech.compliance.service;

import com.fintech.compliance.config.ComplianceProperties;
import com.fintech.compliance.provider.GigaChatProvider;
import com.fintech.compliance.provider.MockLlmProvider;
import com.fintech.compliance.provider.YandexGptProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Фабрика LLM-провайдеров. Возвращает активного провайдера по конфигу.
 * Расширение списка провайдеров делается через DI без изменения оркестратора.
 */
@Component
public class LlmProviderFactory {

    private final ComplianceProperties properties;
    private final Map<String, LlmProvider> providers;

    public LlmProviderFactory(ComplianceProperties properties,
                              MockLlmProvider mock,
                              YandexGptProvider yandex,
                              GigaChatProvider giga) {
        this.properties = properties;
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
            throw new IllegalArgumentException(
                    "Неизвестный LLM_PROVIDER: " + requested + ". Доступно: " + providers.keySet());
        }
        return provider;
    }

    public List<String> available() {
        return List.copyOf(providers.keySet());
    }
}