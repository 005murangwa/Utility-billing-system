package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.TariffResponse;
import com.ubs.billing.entity.Tariff;

public final class TariffMapper {

    private TariffMapper() {
    }

    public static TariffResponse toResponse(Tariff tariff) {
        return TariffResponse.builder()
                .id(tariff.getId())
                .meterType(tariff.getMeterType())
                .rate(tariff.getRate())
                .serviceCharge(tariff.getServiceCharge())
                .vat(tariff.getVat())
                .penaltyRate(tariff.getPenaltyRate())
                .version(tariff.getVersion())
                .effectiveDate(tariff.getEffectiveDate())
                .active(tariff.getActive())
                .createdAt(tariff.getCreatedAt())
                .updatedAt(tariff.getUpdatedAt())
                .build();
    }
}
