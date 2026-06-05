package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.entity.Bill;

public final class BillMapper {

    private BillMapper() {
    }

    public static BillResponse toResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billReference(bill.getBillReference())
                .customerId(bill.getCustomer().getId())
                .customerFullName(bill.getCustomer().getFullName())
                .meterId(bill.getMeter().getId())
                .meterNumber(bill.getMeter().getMeterNumber())
                .meterType(bill.getMeter().getMeterType())
                .tariffId(bill.getTariff().getId())
                .tariffVersion(bill.getTariff().getVersion())
                .meterReadingId(bill.getMeterReading().getId())
                .month(bill.getMonth())
                .year(bill.getYear())
                .consumption(bill.getConsumption())
                .amount(bill.getAmount())
                .vatAmount(bill.getVatAmount())
                .penaltyAmount(bill.getPenaltyAmount())
                .serviceCharge(bill.getServiceCharge())
                .totalAmount(bill.getTotalAmount())
                .balance(bill.getBalance())
                .status(bill.getStatus())
                .approved(bill.getApproved())
                .generatedDate(bill.getGeneratedDate())
                .dueDate(bill.getDueDate())
                .latePenaltyApplied(bill.getLatePenaltyApplied())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}
