package com.ubs.billing.controller;

import com.ubs.billing.dto.request.CreateMeterRequest;
import com.ubs.billing.dto.request.UpdateMeterRequest;
import com.ubs.billing.dto.response.MeterResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.service.MeterService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
@Tag(name = "Meters", description = "Meter management endpoints (Admin and Operator)")
@SecurityRequirement(name = "Bearer Authentication")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @Operation(summary = "Create meter", description = "Creates a new meter assigned to a customer. Meter numbers must be unique.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Meter created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Customer not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate meter number", content = @Content)
    })
    public ResponseEntity<ApiResponse<MeterResponse>> createMeter(
            @Valid @RequestBody CreateMeterRequest request) {
        MeterResponse response = meterService.createMeter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meter", description = "Updates an existing meter and enforces unique meter number constraint")
    public ResponseEntity<ApiResponse<MeterResponse>> updateMeter(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMeterRequest request) {
        MeterResponse response = meterService.updateMeter(id, request);
        return ResponseEntity.ok(ApiResponse.success("Meter updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete meter", description = "Permanently deletes a meter by ID")
    public ResponseEntity<ApiResponse<Void>> deleteMeter(@PathVariable Long id) {
        meterService.deleteMeter(id);
        return ResponseEntity.ok(ApiResponse.success("Meter deleted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View meter", description = "Retrieves a meter by ID including customer details")
    public ResponseEntity<ApiResponse<MeterResponse>> getMeterById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(meterService.getMeterById(id)));
    }

    @GetMapping
    @Operation(
            summary = "Get all meters",
            description = "Returns paginated meters with optional search by meter number, type, status, or customer ID. "
                    + "Supports sorting via sort parameter (e.g. sort=installationDate,desc)."
    )
    public ResponseEntity<ApiResponse<PageResponse<MeterResponse>>> getAllMeters(
            @Parameter(description = "Search by meter number (partial match)")
            @RequestParam(required = false) String meterNumber,
            @Parameter(description = "Filter by meter type")
            @RequestParam(required = false) MeterType meterType,
            @Parameter(description = "Filter by meter status")
            @RequestParam(required = false) MeterStatus status,
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<MeterResponse> response = meterService.getAllMeters(
                meterNumber, meterType, status, customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(
            summary = "View customer meters",
            description = "Returns all meters owned by a customer with pagination, sorting, and optional search filters"
    )
    public ResponseEntity<ApiResponse<PageResponse<MeterResponse>>> getCustomerMeters(
            @PathVariable Long customerId,
            @Parameter(description = "Search by meter number (partial match)")
            @RequestParam(required = false) String meterNumber,
            @Parameter(description = "Filter by meter type")
            @RequestParam(required = false) MeterType meterType,
            @Parameter(description = "Filter by meter status")
            @RequestParam(required = false) MeterStatus status,
            @PageableDefault(size = 20, sort = "installationDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<MeterResponse> response = meterService.getCustomerMeters(
                customerId, meterNumber, meterType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
