package com.ubs.billing.dto.response;

import com.ubs.billing.entity.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String entityName;
    private Long entityId;
    private String performedBy;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
}
