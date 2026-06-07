package com.ubs.billing.controller;

import com.ubs.billing.dto.request.CreatePaymentRequest;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.entity.PaymentMethod;
import com.ubs.billing.service.PaymentService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FINANCE')")
@Tag(name = "Payments", description = "Payment processing endpoints (Finance only)")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(
            summary = "Create payment",
            description = "Records a partial or full payment against an approved bill. "
                    + "Prevents overpayment and automatically updates bill balance and status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment recorded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or overpayment", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentById(id)));
    }

    @GetMapping
    @Operation(
            summary = "Get payments",
            description = "Returns paginated payments with optional filters by bill, customer, method, or bill reference."
    )
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(
            @Parameter(description = "Filter by bill ID")
            @RequestParam(required = false) Long billId,
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Filter by payment method")
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "Search by bill reference (partial match)")
            @RequestParam(required = false) String billReference,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<PaymentResponse> response = paymentService.getPayments(
                billId, customerId, paymentMethod, billReference, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Customer payment history", description = "Returns all payments made by a specific customer")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getCustomerPaymentHistory(
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<PaymentResponse> response = paymentService.getCustomerPaymentHistory(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/bills/{billId}/send-paid-receipt")
    @Operation(
            summary = "Send paid receipt email",
            description = "Sends a payment confirmation email to the customer for a fully paid bill."
    )
    public ResponseEntity<ApiResponse<Void>> sendPaidReceiptEmail(@PathVariable Long billId) {
        paymentService.sendPaidReceiptEmail(billId);
        return ResponseEntity.ok(ApiResponse.success("Paid receipt email sent"));
    }
}
