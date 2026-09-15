package com.fintech.compliance;

import com.fintech.compliance.config.ComplianceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Точка входа микросервиса FinTech Compliance AI Orchestrator.
 * Оркестратор AML-проверок с локальной анонимизацией данных по 152-ФЗ.
 */
@SpringBootApplication
@EnableConfigurationProperties(ComplianceProperties.class)
public class ComplianceOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplianceOrchestratorApplication.class, args);
    }
}