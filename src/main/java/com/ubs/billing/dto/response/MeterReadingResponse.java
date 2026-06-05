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
public class MeterReadingResponse {

    private Long id;
    private Long meterId;
    private String meterNumber;
    private MeterType meterType;
    private Long customerId;
    private String customerFullName;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private BigDecimal consumption;
    private LocalDate readingDate;
    private Integer month;
    private Integer year;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
