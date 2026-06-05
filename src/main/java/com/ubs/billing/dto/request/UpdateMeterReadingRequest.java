package com.ubs.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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
@Schema(description = "Update meter reading request")
public class UpdateMeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    private Long meterId;

    @NotNull(message = "Previous reading is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Previous reading must be zero or greater")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Current reading must be zero or greater")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    private LocalDate readingDate;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    private Integer year;
}
