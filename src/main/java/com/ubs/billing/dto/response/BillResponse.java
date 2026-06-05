package com.ubs.billing.dto.response;

import com.ubs.billing.entity.BillStatus;
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
public class BillResponse {

    private Long id;
    private String billReference;
    private Long customerId;
    private String customerFullName;
    private Long meterId;
    private String meterNumber;
    private MeterType meterType;
    private Long tariffId;
    private Integer tariffVersion;
    private Long meterReadingId;
    private Integer month;
    private Integer year;
    private BigDecimal consumption;
    private BigDecimal amount;
    private BigDecimal vatAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal serviceCharge;
    private BigDecimal totalAmount;
    private BigDecimal balance;
    private BillStatus status;
    private Boolean approved;
    private LocalDate generatedDate;
    private LocalDate dueDate;
    private Boolean latePenaltyApplied;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
