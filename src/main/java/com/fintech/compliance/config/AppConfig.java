package com.fintech.compliance.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Конфигурация HTTP-клиента для интеграций с LLM-провайдерами.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Поддержка Swagger UI в Spring Boot 3.x
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .resourceChain(true);
    }
}