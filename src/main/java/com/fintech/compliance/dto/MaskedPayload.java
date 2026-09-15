package com.fintech.compliance.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Контейнер анонимизированного текста с картой замен.
 * token -> originalValue (для последующей деанонимизации).
 */
public class MaskedPayload {

    private final String maskedText;
    private final Map<String, String> tokenMap;

    public MaskedPayload(String maskedText, Map<String, String> tokenMap) {
        this.maskedText = maskedText;
        this.tokenMap = new LinkedHashMap<>(tokenMap);
    }

    public String getMaskedText() { return maskedText; }
    public Map<String, String> getTokenMap() { return tokenMap; }
}