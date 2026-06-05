package com.ubs.billing.dto.request;

import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update meter request")
public class UpdateMeterRequest {

    @NotBlank(message = "Meter number is required")
    @Size(max = 50, message = "Meter number must not exceed 50 characters")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    private LocalDate installationDate;

    @NotNull(message = "Status is required")
    private MeterStatus status;

    @NotNull(message = "Customer ID is required")
    private Long customerId;
}
