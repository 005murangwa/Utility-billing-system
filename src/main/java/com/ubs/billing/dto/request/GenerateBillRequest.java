package com.ubs.billing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generate bill request based on a meter reading")
public class GenerateBillRequest {

    @NotNull(message = "Meter reading ID is required")
    @Schema(description = "ID of the meter reading used to calculate consumption")
    private Long meterReadingId;
}
