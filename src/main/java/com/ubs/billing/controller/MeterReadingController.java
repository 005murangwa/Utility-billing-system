package com.ubs.billing.controller;

import com.ubs.billing.dto.request.CreateMeterReadingRequest;
import com.ubs.billing.dto.request.UpdateMeterReadingRequest;
import com.ubs.billing.dto.response.MeterReadingResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.service.MeterReadingService;
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
@RequestMapping("/meter-readings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OPERATOR')")
@Tag(name = "Meter Readings", description = "Meter reading management endpoints (Operator only)")
@SecurityRequirement(name = "Bearer Authentication")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @Operation(
            summary = "Create meter reading",
            description = "Records a new reading for an active meter. Only one reading per meter per month/year is allowed."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reading created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation or business rule violation", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Meter not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate reading or inactive meter", content = @Content)
    })
    public ResponseEntity<ApiResponse<MeterReadingResponse>> createReading(
            @Valid @RequestBody CreateMeterReadingRequest request) {
        MeterReadingResponse response = meterReadingService.createReading(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter reading created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update meter reading", description = "Updates an existing meter reading with full validation")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> updateReading(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMeterReadingRequest request) {
        MeterReadingResponse response = meterReadingService.updateReading(id, request);
        return ResponseEntity.ok(ApiResponse.success("Meter reading updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete meter reading", description = "Permanently deletes a meter reading by ID")
    public ResponseEntity<ApiResponse<Void>> deleteReading(@PathVariable Long id) {
        meterReadingService.deleteReading(id);
        return ResponseEntity.ok(ApiResponse.success("Meter reading deleted successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meter reading", description = "Retrieves a meter reading by ID")
    public ResponseEntity<ApiResponse<MeterReadingResponse>> getReadingById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(meterReadingService.getReadingById(id)));
    }

    @GetMapping
    @Operation(
            summary = "List meter readings",
            description = "Returns paginated meter readings with optional filters by meter ID, month, year, or meter number. "
                    + "Supports sorting via sort parameter (e.g. sort=readingDate,desc)."
    )
    public ResponseEntity<ApiResponse<PageResponse<MeterReadingResponse>>> listReadings(
            @Parameter(description = "Filter by meter ID")
            @RequestParam(required = false) Long meterId,
            @Parameter(description = "Filter by month (1-12)")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Filter by year")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Search by meter number (partial match)")
            @RequestParam(required = false) String meterNumber,
            @PageableDefault(size = 20, sort = "readingDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<MeterReadingResponse> response = meterReadingService.listReadings(
                meterId, month, year, meterNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
