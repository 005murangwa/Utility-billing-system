package com.ubs.billing.dto.response;

import com.ubs.billing.entity.NotificationStatus;
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
public class NotificationResponse {

    private Long id;
    private Long customerId;
    private String customerFullName;
    private String message;
    private NotificationStatus status;
    private LocalDateTime createdAt;
}
