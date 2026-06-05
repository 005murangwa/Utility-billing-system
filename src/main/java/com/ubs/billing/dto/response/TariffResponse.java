package com.ubs.billing.dto.response;

import com.ubs.billing.entity.MeterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TariffResponse {

    private Long id;
    private MeterType meterType;
    private BigDecimal rate;
    private BigDecimal serviceCharge;
    private BigDecimal vat;
    private BigDecimal penaltyRate;
    private Integer version;
    private LocalDate effectiveDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
