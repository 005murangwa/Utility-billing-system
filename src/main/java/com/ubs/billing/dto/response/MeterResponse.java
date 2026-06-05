package com.ubs.billing.dto.response;

import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterResponse {

    private Long id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private MeterStatus status;
    private Long customerId;
    private String customerFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
