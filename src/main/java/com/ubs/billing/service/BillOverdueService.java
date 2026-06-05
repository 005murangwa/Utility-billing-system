package com.ubs.billing.service;

import com.ubs.billing.config.BillingProperties;
import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.Meter;
import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.NotificationEventType;
import com.ubs.billing.entity.Tariff;
import com.ubs.billing.repository.BillRepository;
import com.ubs.billing.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillOverdueService {

    private static final Set<BillStatus> OVERDUE_ELIGIBLE_STATUSES = EnumSet.of(
            BillStatus.APPROVED,
            BillStatus.PENDING,
            BillStatus.PARTIALLY_PAID
    );

    private final BillRepository billRepository;
    private final MeterRepository meterRepository;
    private final NotificationService notificationService;
    private final BillingProperties billingProperties;

    @Scheduled(cron = "${app.billing.overdue-cron:0 0 1 * * *}")
    @Transactional
    public void processOverdueBills() {
        LocalDate today = LocalDate.now();
        List<Bill> overdueCandidates = billRepository.findOverdueCandidates(today, OVERDUE_ELIGIBLE_STATUSES);

        for (Bill bill : overdueCandidates) {
            applyLatePenaltyIfNeeded(bill);
            disconnectMeterIfNeeded(bill, today);
        }
    }

    private void applyLatePenaltyIfNeeded(Bill bill) {
        if (Boolean.TRUE.equals(bill.getLatePenaltyApplied())) {
            if (bill.getStatus() != BillStatus.PAID && bill.getStatus() != BillStatus.OVERDUE) {
                bill.setStatus(BillStatus.OVERDUE);
                billRepository.save(bill);
            }
            return;
        }

        Tariff tariff = bill.getTariff();
        BigDecimal latePenalty = bill.getConsumption()
                .multiply(tariff.getPenaltyRate())
                .setScale(2, RoundingMode.HALF_UP);

        bill.setPenaltyAmount(bill.getPenaltyAmount().add(latePenalty));
        bill.setTotalAmount(bill.getTotalAmount().add(latePenalty));
        bill.setBalance(bill.getBalance().add(latePenalty));
        bill.setLatePenaltyApplied(true);
        bill.setStatus(BillStatus.OVERDUE);
        billRepository.save(bill);

        Customer customer = bill.getCustomer();
        String message = "Dear " + customer.getFullName() + ", your bill " + bill.getBillReference()
                + " is overdue. A late payment penalty of " + latePenalty + " FRW has been applied.";
        notificationService.createNotificationIfAbsent(
                customer,
                message,
                NotificationEventType.BILL_OVERDUE,
                bill.getId());

        log.info("Applied late penalty to bill {}", bill.getBillReference());
    }

    private void disconnectMeterIfNeeded(Bill bill, LocalDate today) {
        if (bill.getDueDate() == null) {
            return;
        }

        long daysOverdue = ChronoUnit.DAYS.between(bill.getDueDate(), today);
        if (daysOverdue < billingProperties.getOverdueDisconnectDays()) {
            return;
        }

        Meter meter = bill.getMeter();
        if (meter.getStatus() == MeterStatus.DISCONNECTED) {
            return;
        }

        meter.setStatus(MeterStatus.DISCONNECTED);
        meterRepository.save(meter);

        Customer customer = bill.getCustomer();
        String message = "Dear " + customer.getFullName() + ", meter " + meter.getMeterNumber()
                + " has been disconnected due to prolonged overdue payment on bill " + bill.getBillReference() + ".";
        notificationService.createNotificationIfAbsent(
                customer,
                message,
                NotificationEventType.METER_DISCONNECTED,
                meter.getId());

        log.info("Disconnected meter {} due to overdue bill {}", meter.getMeterNumber(), bill.getBillReference());
    }
}
