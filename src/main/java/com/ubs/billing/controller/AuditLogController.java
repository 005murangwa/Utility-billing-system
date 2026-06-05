package com.ubs.billing.controller;

import com.ubs.billing.dto.response.AuditLogResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.service.AuditLogService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
@Tag(name = "Audit Logs", description = "Audit trail for users, customers, meters, readings, tariffs, bills, and payments")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAuditLogById(id)));
    }

    @GetMapping
    @Operation(
            summary = "Search audit logs",
            description = "Returns paginated audit logs with filters for action, entity (Customer, Meter, Bill, Payment), "
                    + "entity ID, performer, and free-text search. Supports sorting (e.g. sort=timestamp,desc)."
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> searchAuditLogs(
            @Parameter(description = "Filter by action: CREATE, UPDATE, DELETE, APPROVE")
            @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Filter by entity name: Customer, Meter, Bill, Payment")
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String performedBy,
            @Parameter(description = "Search across action, entity name, and performer")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<AuditLogResponse> response = auditLogService.searchAuditLogs(
                action, entityName, entityId, performedBy, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
