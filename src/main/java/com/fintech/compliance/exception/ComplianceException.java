package com.fintech.compliance.exception;

/**
 * Базовое доменное исключение модуля compliance.
 */
public class ComplianceException extends RuntimeException {

    public ComplianceException(String message) {
        super(message);
    }

    public ComplianceException(String message, Throwable cause) {
        super(message, cause);
    }
}