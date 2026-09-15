package com.fintech.compliance.service;

import com.fintech.compliance.dto.MaskedPayload;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис локальной анонимизации/деанонимизации.
 * Заменяет чувствительные сущности на токены вида [TOKEN_XXXX].
 * Никакие исходные данные не покидают периметр JVM — это основа 152-ФЗ.
 */
@Service
public class DataMaskingService {

    private static final String TOKEN_PATTERN_STR =
            "\\b(?:7\\d{10}|\\+7\\d{10}|\\d{16}|\\d{12}|\\d{10}|[A-Z]{2}\\d{9})\\b"
                    + "|\\b[\\p{Lu}][\\p{Ll}]+\\s+[\\p{Lu}][\\p{Ll}]+\\s+[\\p{Lu}][\\p{Ll}]+\\b"
                    + "|\\b[\\p{Lu}][\\p{Ll}]{1,20}\\s+[\\p{Lu}][\\p{Ll}]{1,20}\\b";

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(TOKEN_PATTERN_STR);

    private final AtomicLong counter = new AtomicLong(0);

    /**
     * Анонимизировать текст: заменить персональные данные токенами.
     */
    public MaskedPayload anonymize(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new MaskedPayload("", new LinkedHashMap<>());
        }
        Map<String, String> tokenMap = new LinkedHashMap<>();
        StringBuffer buffer = new StringBuffer();
        Matcher matcher = SENSITIVE_PATTERN.matcher(rawText);

        while (matcher.find()) {
            String original = matcher.group();
            String token = "[TOKEN_" + counter.incrementAndGet() + "]";
            tokenMap.put(token, original);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(token));
        }
        matcher.appendTail(buffer);
        return new MaskedPayload(buffer.toString(), tokenMap);
    }

    /**
     * Восстановить оригинальные сущности в тексте отчёта.
     */
    public String deanonymize(String text, Map<String, String> tokenMap) {
        if (text == null || text.isEmpty() || tokenMap == null || tokenMap.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> e : tokenMap.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }
}