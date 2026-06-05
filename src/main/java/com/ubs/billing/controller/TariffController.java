package com.ubs.billing.controller;

import com.ubs.billing.dto.request.CreateTariffRequest;
import com.ubs.billing.dto.request.UpdateTariffRequest;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.dto.response.TariffResponse;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.service.TariffService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Tariffs", description = "Tariff management with versioning (Admin only)")
@SecurityRequirement(name = "Bearer Authentication")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @Operation(
            summary = "Create tariff",
            description = "Creates a new tariff version for a meter type. "
                    + "Version is auto-incremented. Activating a new tariff deactivates prior active tariffs for the same meter type."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tariff created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ApiResponse<TariffResponse>> createTariff(
            @Valid @RequestBody CreateTariffRequest request) {
        TariffResponse response = tariffService.createTariff(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tariff created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update tariff",
            description = "Creates a new tariff version without modifying historical records. "
                    + "The meter type is inherited from the referenced tariff."
    )
    public ResponseEntity<ApiResponse<TariffResponse>> updateTariff(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTariffRequest request) {
        TariffResponse response = tariffService.updateTariff(id, request);
        return ResponseEntity.ok(ApiResponse.success("New tariff version created successfully", response));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate tariff",
            description = "Deactivates a tariff without deleting it. Historical records remain unchanged."
    )
    public ResponseEntity<ApiResponse<TariffResponse>> deactivateTariff(@PathVariable Long id) {
        TariffResponse response = tariffService.deactivateTariff(id);
        return ResponseEntity.ok(ApiResponse.success("Tariff deactivated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View tariff by ID")
    public ResponseEntity<ApiResponse<TariffResponse>> getTariffById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getTariffById(id)));
    }

    @GetMapping("/active/{meterType}")
    @Operation(
            summary = "View latest active tariff",
            description = "Returns the latest active tariff for a meter type effective on or before today. Used for future billing."
    )
    public ResponseEntity<ApiResponse<TariffResponse>> getLatestActiveTariff(@PathVariable MeterType meterType) {
        return ResponseEntity.ok(ApiResponse.success(tariffService.getLatestActiveTariff(meterType)));
    }

    @GetMapping
    @Operation(
            summary = "View tariffs",
            description = "Returns paginated tariffs with optional filters by meter type and active status. "
                    + "Supports sorting via sort parameter (e.g. sort=version,desc)."
    )
    public ResponseEntity<ApiResponse<PageResponse<TariffResponse>>> getTariffs(
            @Parameter(description = "Filter by meter type")
            @RequestParam(required = false) MeterType meterType,
            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "version", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<TariffResponse> response = tariffService.getTariffs(meterType, active, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
