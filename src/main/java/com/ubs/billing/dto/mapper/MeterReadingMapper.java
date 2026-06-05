package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.MeterReadingResponse;
import com.ubs.billing.entity.MeterReading;

public final class MeterReadingMapper {

    private MeterReadingMapper() {
    }

    public static MeterReadingResponse toResponse(MeterReading reading) {
        return MeterReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
                .meterNumber(reading.getMeter().getMeterNumber())
                .meterType(reading.getMeter().getMeterType())
                .customerId(reading.getMeter().getCustomer().getId())
                .customerFullName(reading.getMeter().getCustomer().getFullName())
                .previousReading(reading.getPreviousReading())
                .currentReading(reading.getCurrentReading())
                .consumption(reading.getConsumption())
                .readingDate(reading.getReadingDate())
                .month(reading.getMonth())
                .year(reading.getYear())
                .createdAt(reading.getCreatedAt())
                .updatedAt(reading.getUpdatedAt())
                .build();
    }
}
