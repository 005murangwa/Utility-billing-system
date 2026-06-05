package com.ubs.billing.repository;

import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:entityName IS NULL OR a.entityName = :entityName)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:performedBy = '' OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :performedBy, '%')))
              AND (:search = '' OR LOWER(a.entityName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AuditLog> searchAuditLogs(
            @Param("action") AuditAction action,
            @Param("entityName") String entityName,
            @Param("entityId") Long entityId,
            @Param("performedBy") String performedBy,
            @Param("search") String search,
            Pageable pageable);
}
