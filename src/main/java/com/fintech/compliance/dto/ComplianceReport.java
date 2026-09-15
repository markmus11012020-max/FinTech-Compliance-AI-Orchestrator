package com.fintech.compliance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Финальный (деанонимизированный) отчёт о compliance-проверке.
 * Жёсткий JSON-layout согласно требованиям ЦБ РФ.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplianceReport {
    private boolean isSuspicious;
    private String riskLevel;
    private String reason;

    public ComplianceReport() {}

    public ComplianceReport(boolean isSuspicious, String riskLevel, String reason) {
        this.isSuspicious = isSuspicious;
        this.riskLevel = riskLevel;
        this.reason = reason;
    }

    public boolean isSuspicious() { return isSuspicious; }
    public void setSuspicious(boolean suspicious) { isSuspicious = suspicious; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static ComplianceReport safe(String reason) {
        return new ComplianceReport(false, "LOW", reason);
    }
}