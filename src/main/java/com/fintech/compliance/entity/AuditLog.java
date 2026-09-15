package com.fintech.compliance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Журнал аудита compliance-проверок.
 * Хранит только метаданные: провайдер, кол-во токенов, риск-уровень.
 * Реальные ПДн и оригинальные имена НЕ сохраняются.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    @Column(name = "provider", length = 32, nullable = false)
    private String provider;

    @Column(name = "masked_entities_count", nullable = false)
    private int maskedEntitiesCount;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "suspicious")
    private Boolean suspicious;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getMaskedEntitiesCount() { return maskedEntitiesCount; }
    public void setMaskedEntitiesCount(int maskedEntitiesCount) { this.maskedEntitiesCount = maskedEntitiesCount; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getSuspicious() { return suspicious; }
    public void setSuspicious(Boolean suspicious) { this.suspicious = suspicious; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}