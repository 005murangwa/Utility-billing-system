package com.ubs.billing.controller;

import com.ubs.billing.dto.request.GenerateBillRequest;
import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.service.BillService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
@Tag(name = "Bills", description = "Bill generation and management (Admin and Finance)")
@SecurityRequirement(name = "Bearer Authentication")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @Operation(
            summary = "Generate bill",
            description = "Generates a bill from a meter reading applying tariff rate, VAT, service charge, and penalty. "
                    + "Inactive customers cannot receive bills. Only one bill per meter/month/year is allowed."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Bill generated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Reading, meter, customer, or tariff not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate bill or inactive customer/meter", content = @Content)
    })
    public ResponseEntity<ApiResponse<BillResponse>> generateBill(
            @Valid @RequestBody GenerateBillRequest request) {
        BillResponse response = billService.generateBill(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill generated successfully", response));
    }

    @PatchMapping("/{id}/approve")
    @Operation(
            summary = "Approve bill",
            description = "Approves a pending bill for issuance. Re-validates customer and meter eligibility."
    )
    public ResponseEntity<ApiResponse<BillResponse>> approveBill(@PathVariable Long id) {
        BillResponse response = billService.approveBill(id);
        return ResponseEntity.ok(ApiResponse.success("Bill approved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(billService.getBillById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List bills",
            description = "Returns paginated bills with optional filters. Supports sorting (e.g. sort=generatedDate,desc)."
    )
    public ResponseEntity<ApiResponse<PageResponse<BillResponse>>> listBills(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Filter by meter ID")
            @RequestParam(required = false) Long meterId,
            @Parameter(description = "Filter by month (1-12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Filter by bill status")
            @RequestParam(required = false) BillStatus status,
            @Parameter(description = "Search by bill reference (partial match)")
            @RequestParam(required = false) String billReference,
            @Parameter(description = "Filter by approval status")
            @RequestParam(required = false) Boolean approved,
            @PageableDefault(size = 20, sort = "generatedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<BillResponse> response = billService.listBills(
                customerId, meterId, month, year, status, billReference, approved, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer bills", description = "Returns all bills for a specific customer")
    public ResponseEntity<ApiResponse<PageResponse<BillResponse>>> getCustomerBills(
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) BillStatus status,
            @PageableDefault(size = 20, sort = "generatedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<BillResponse> response = billService.getCustomerBills(
                customerId, month, year, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
