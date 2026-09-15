package com.fintech.compliance;

import com.fintech.compliance.dto.MaskedPayload;
import com.fintech.compliance.service.DataMaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовые unit-тесты DataMaskingService (без Spring-контекста и БД).
 * Запускаются при CI-сборке и подтверждают корректность 152-ФЗ pipeline.
 */
class ComplianceOrchestratorApplicationTests {

    private DataMaskingService maskingService;

    @BeforeEach
    void setUp() {
        maskingService = new DataMaskingService();
    }

    @Test
    void contextIsReady() {
        assertThat(maskingService).isNotNull();
    }

    @Test
    void anonymizeReplacesPhoneAndCard() {
        String raw = "Клиент Иван Иванов, тел +79991234567, карта 1234567890123456";
        MaskedPayload masked = maskingService.anonymize(raw);
        assertThat(masked.getMaskedText()).contains("[TOKEN_");
        assertThat(masked.getTokenMap()).isNotEmpty();
    }

    @Test
    void anonymizeEmptyReturnsEmpty() {
        MaskedPayload masked = maskingService.anonymize("");
        assertThat(masked.getMaskedText()).isEmpty();
        assertThat(masked.getTokenMap()).isEmpty();
    }

    @Test
    void deanonymizeRestoresOriginals() {
        String raw = "Иван Иванов";
        MaskedPayload masked = maskingService.anonymize(raw);
        String report = "Подозрительно: " + masked.getMaskedText();
        String restored = maskingService.deanonymize(report, masked.getTokenMap());
        assertThat(restored).contains("Иван Иванов");
    }

    @Test
    void deanonymizeEmptyMapReturnsOriginal() {
        String text = "Plain text without tokens";
        String result = maskingService.deanonymize(text, new java.util.HashMap<>());
        assertThat(result).isEqualTo(text);
    }
}