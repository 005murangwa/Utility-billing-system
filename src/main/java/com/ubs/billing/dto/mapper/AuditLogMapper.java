package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.AuditLogResponse;
import com.ubs.billing.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .performedBy(auditLog.getPerformedBy())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
