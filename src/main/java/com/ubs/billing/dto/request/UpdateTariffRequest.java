package com.ubs.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update tariff request. Creates a new tariff version; historical records remain unchanged.")
public class UpdateTariffRequest {

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must not be negative")
    private BigDecimal rate;

    @NotNull(message = "Service charge is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Service charge must not be negative")
    private BigDecimal serviceCharge;

    @NotNull(message = "VAT is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "VAT must not be negative")
    private BigDecimal vat;

    @NotNull(message = "Penalty rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Penalty rate must not be negative")
    private BigDecimal penaltyRate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @NotNull(message = "Active flag is required")
    private Boolean active;
}
