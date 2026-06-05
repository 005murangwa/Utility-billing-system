package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.MeterResponse;
import com.ubs.billing.entity.Meter;

public final class MeterMapper {

    private MeterMapper() {
    }

    public static MeterResponse toResponse(Meter meter) {
        return MeterResponse.builder()
                .id(meter.getId())
                .meterNumber(meter.getMeterNumber())
                .meterType(meter.getMeterType())
                .installationDate(meter.getInstallationDate())
                .status(meter.getStatus())
                .customerId(meter.getCustomer().getId())
                .customerFullName(meter.getCustomer().getFullName())
                .createdAt(meter.getCreatedAt())
                .updatedAt(meter.getUpdatedAt())
                .build();
    }
}
