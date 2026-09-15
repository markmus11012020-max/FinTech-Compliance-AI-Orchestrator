package com.fintech.compliance.dto;

/**
 * Входящий запрос на AML-проверку транзакции.
 */
public class CheckRequest {
    private String transactionId;
    private String payload;

    public CheckRequest() {}

    public CheckRequest(String transactionId, String payload) {
        this.transactionId = transactionId;
        this.payload = payload;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}