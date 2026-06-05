package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .billReference(payment.getBill().getBillReference())
                .customerId(payment.getBill().getCustomer().getId())
                .customerFullName(payment.getBill().getCustomer().getFullName())
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .billBalanceAfterPayment(payment.getBill().getBalance())
                .billStatusAfterPayment(payment.getBill().getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
