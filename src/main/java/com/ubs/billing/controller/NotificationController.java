package com.ubs.billing.controller;

import com.ubs.billing.dto.response.NotificationResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.NotificationStatus;
import com.ubs.billing.service.NotificationService;
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
@RequestMapping("/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Notifications", description = "Customer notification endpoints (Customer only)")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "Get notifications",
            description = "Returns paginated notifications for the authenticated customer. "
                    + "Notifications are created automatically when bills are generated or fully paid."
    )
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @Parameter(description = "Filter by notification status")
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<NotificationResponse> response = notificationService.getNotifications(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Returns a single notification for the authenticated customer")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationById(id)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "Customer notifications",
            description = "Returns notifications for a customer. Customers may only access their own notifications."
    )
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getCustomerNotifications(
            @PathVariable Long customerId,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<NotificationResponse> response = notificationService.getCustomerNotifications(
                customerId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
