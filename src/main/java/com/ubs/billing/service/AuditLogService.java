package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.AuditLogMapper;
import com.ubs.billing.dto.response.AuditLogResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.AuditLog;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.AuditLogRepository;
import com.ubs.billing.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityName, Long entityId) {
        log(action, entityName, entityId, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityName, Long entityId, String oldValue, String newValue) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .performedBy(SecurityUtils.getCurrentUsername())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAuditLogs(
            AuditAction action,
            String entityName,
            Long entityId,
            String performedBy,
            String search,
            Pageable pageable) {

        Page<AuditLogResponse> page = auditLogRepository
                .searchAuditLogs(
                        action,
                        normalizeSearchParam(entityName),
                        entityId,
                        normalizeSearchParam(performedBy),
                        normalizeSearchParam(search),
                        pageable)
                .map(AuditLogMapper::toResponse);

        return PageResponse.from(page);
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
