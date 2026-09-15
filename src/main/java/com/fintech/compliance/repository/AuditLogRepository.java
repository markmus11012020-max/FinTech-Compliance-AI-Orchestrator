package com.fintech.compliance.repository;

import com.fintech.compliance.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий аудита. Spring Data JPA сгенерирует CRUD-методы автоматически.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}