package com.ubs.billing.dto.response;

import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.PaymentMethod;
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
public class PaymentResponse {

    private Long id;
    private Long billId;
    private String billReference;
    private Long customerId;
    private String customerFullName;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private BigDecimal billBalanceAfterPayment;
    private BillStatus billStatusAfterPayment;
    private LocalDateTime createdAt;
}
