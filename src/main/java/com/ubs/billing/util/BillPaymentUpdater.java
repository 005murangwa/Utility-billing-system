package com.ubs.billing.util;

import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.BillStatus;

import java.math.BigDecimal;

public final class BillPaymentUpdater {

    private BillPaymentUpdater() {
    }

    public static void applyPayment(Bill bill, BigDecimal amountPaid) {
        BigDecimal newBalance = bill.getBalance().subtract(amountPaid);

        bill.setBalance(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
        } else if (newBalance.compareTo(bill.getTotalAmount()) < 0) {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        } else if (bill.getStatus() == BillStatus.APPROVED) {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        }
    }
}
