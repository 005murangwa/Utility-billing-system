package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.NotificationResponse;
import com.ubs.billing.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .customerId(notification.getCustomer().getId())
                .customerFullName(notification.getCustomer().getFullName())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
