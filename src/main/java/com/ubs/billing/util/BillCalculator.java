package com.ubs.billing.util;

import com.ubs.billing.entity.Tariff;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BillCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private BillCalculator() {
    }

    public static BillAmounts calculate(BigDecimal consumption, Tariff tariff) {
        BigDecimal amount = consumption
                .multiply(tariff.getRate())
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal serviceCharge = tariff.getServiceCharge()
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal taxableBase = amount.add(serviceCharge);

        BigDecimal vatAmount = taxableBase
                .multiply(tariff.getVat())
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, ROUNDING);

        BigDecimal penaltyAmount = consumption
                .multiply(tariff.getPenaltyRate())
                .setScale(MONEY_SCALE, ROUNDING);

        BigDecimal totalAmount = amount
                .add(serviceCharge)
                .add(vatAmount)
                .add(penaltyAmount)
                .setScale(MONEY_SCALE, ROUNDING);

        return BillAmounts.builder()
                .consumption(consumption.setScale(MONEY_SCALE, ROUNDING))
                .amount(amount)
                .serviceCharge(serviceCharge)
                .vatAmount(vatAmount)
                .penaltyAmount(penaltyAmount)
                .totalAmount(totalAmount)
                .build();
    }

    @Getter
    @Builder
    public static class BillAmounts {
        private BigDecimal consumption;
        private BigDecimal amount;
        private BigDecimal serviceCharge;
        private BigDecimal vatAmount;
        private BigDecimal penaltyAmount;
        private BigDecimal totalAmount;
    }
}
