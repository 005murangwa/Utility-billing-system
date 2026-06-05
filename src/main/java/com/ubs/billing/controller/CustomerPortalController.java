package com.ubs.billing.controller;

import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.service.BillService;
import com.ubs.billing.service.PaymentService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/my")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Portal", description = "Customer self-service access to own bills and payments")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerPortalController {

    private final BillService billService;
    private final PaymentService paymentService;

    @GetMapping("/bills")
    @Operation(summary = "Get my approved bills", description = "Returns only the authenticated customer's approved bills")
    public ResponseEntity<ApiResponse<PageResponse<BillResponse>>> getMyBills(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) BillStatus status,
            @PageableDefault(size = 20, sort = "generatedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(billService.getCurrentCustomerBills(month, year, status, pageable)));
    }

    @GetMapping("/bills/{id}")
    @Operation(summary = "Get my bill by ID", description = "Returns an approved bill owned by the authenticated customer")
    public ResponseEntity<ApiResponse<BillResponse>> getMyBillById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillByIdForCurrentCustomer(id)));
    }

    @GetMapping("/payments")
    @Operation(summary = "Get my payments", description = "Returns payment history for the authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(paymentService.getCurrentCustomerPayments(pageable)));
    }
}
