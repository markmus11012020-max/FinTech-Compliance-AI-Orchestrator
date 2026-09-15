package com.fintech.compliance.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Конфигурация HTTP-клиента для интеграций с LLM-провайдерами.
 */
@Configuration
public class AppConfig {

    private final ComplianceProperties properties;

    public AppConfig(ComplianceProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofSeconds(properties.getLlm().getTimeoutSeconds());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}