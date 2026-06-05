package com.ubs.billing.service;

import com.ubs.billing.config.BillingProperties;
import com.ubs.billing.dto.mapper.BillMapper;
import com.ubs.billing.dto.request.GenerateBillRequest;
import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.Meter;
import com.ubs.billing.entity.MeterReading;
import com.ubs.billing.entity.NotificationEventType;
import com.ubs.billing.entity.Tariff;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ForbiddenException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.BillRepository;
import com.ubs.billing.repository.UserRepository;
import com.ubs.billing.security.CustomUserDetails;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.util.BillCalculator;
import com.ubs.billing.util.BillReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final CustomerService customerService;
    private final MeterService meterService;
    private final TariffService tariffService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final BillingProperties billingProperties;

    @Transactional
    public BillResponse generateBill(GenerateBillRequest request) {
        MeterReading reading = meterReadingService.getReadingForBilling(request.getMeterReadingId());
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();

        meterService.findActiveMeterOrThrow(meter.getId());
        customerService.findActiveCustomerForBilling(customer.getId());

        if (reading.getConsumption().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Consumption must be greater than zero to generate a bill");
        }

        if (billRepository.existsByMeterIdAndMonthAndYear(meter.getId(), reading.getMonth(), reading.getYear())) {
            throw new ConflictException("A bill already exists for this meter in the specified month and year");
        }

        Tariff tariff = tariffService.findActiveTariffForBilling(
                meter.getMeterType(),
                reading.getReadingDate());

        BillCalculator.BillAmounts amounts = BillCalculator.calculate(reading.getConsumption(), tariff);

        long sequence = billRepository.countByMonthAndYear(reading.getMonth(), reading.getYear()) + 1;
        String billReference = BillReferenceGenerator.generate(
                reading.getYear(),
                reading.getMonth(),
                meter.getId(),
                sequence);

        Bill bill = Bill.builder()
                .billReference(billReference)
                .customer(customer)
                .meter(meter)
                .tariff(tariff)
                .meterReading(reading)
                .month(reading.getMonth())
                .year(reading.getYear())
                .consumption(amounts.getConsumption())
                .amount(amounts.getAmount())
                .vatAmount(amounts.getVatAmount())
                .penaltyAmount(amounts.getPenaltyAmount())
                .serviceCharge(amounts.getServiceCharge())
                .totalAmount(amounts.getTotalAmount())
                .balance(amounts.getTotalAmount())
                .status(BillStatus.PENDING)
                .approved(false)
                .latePenaltyApplied(false)
                .generatedDate(LocalDate.now())
                .build();

        try {
            Bill savedBill = billRepository.save(bill);
            auditLogService.log(AuditAction.CREATE, AuditEntityNames.BILL, savedBill.getId());

            String generatedMessage = "Dear " + customer.getFullName() + ", your " + savedBill.getMonth()
                    + "/" + savedBill.getYear() + " utility bill " + savedBill.getBillReference()
                    + " has been generated and is pending approval.";
            notificationService.createNotificationIfAbsent(
                    customer,
                    generatedMessage,
                    NotificationEventType.BILL_GENERATED,
                    savedBill.getId());

            return BillMapper.toResponse(findBillWithDetailsOrThrow(savedBill.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A bill already exists for this meter in the specified month and year");
        }
    }

    @Transactional
    public BillResponse approveBill(Long id) {
        Bill bill = findBillWithDetailsOrThrow(id);

        if (Boolean.TRUE.equals(bill.getApproved())) {
            throw new BadRequestException("Bill is already approved");
        }

        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BadRequestException("Only pending bills can be approved");
        }

        customerService.findActiveCustomerForBilling(bill.getCustomer().getId());
        meterService.findActiveMeterOrThrow(bill.getMeter().getId());

        bill.setApproved(true);
        bill.setStatus(BillStatus.APPROVED);
        bill.setDueDate(LocalDate.now().plusDays(billingProperties.getDueDays()));
        Bill approvedBill = billRepository.save(bill);

        auditLogService.log(
                AuditAction.APPROVE,
                AuditEntityNames.BILL,
                approvedBill.getId(),
                "{\"approved\":\"false\"}",
                "{\"approved\":\"true\",\"status\":\"APPROVED\"}");

        userRepository.findByEmail(approvedBill.getCustomer().getEmail())
                .ifPresent(user -> emailService.sendBillApprovedEmail(user, approvedBill));

        return BillMapper.toResponse(approvedBill);
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Long id) {
        return BillMapper.toResponse(findBillWithDetailsOrThrow(id));
    }

    @Transactional(readOnly = true)
    public BillResponse getBillByIdForCurrentCustomer(Long id) {
        Bill bill = findBillWithDetailsOrThrow(id);
        verifyCustomerBillAccess(bill);
        return BillMapper.toResponse(bill);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> listBills(
            Long customerId,
            Long meterId,
            Integer month,
            Integer year,
            BillStatus status,
            String billReference,
            Boolean approved,
            Pageable pageable) {

        Page<BillResponse> page = billRepository
                .searchBills(
                        customerId,
                        meterId,
                        month,
                        year,
                        status,
                        normalizeSearchParam(billReference),
                        approved,
                        pageable)
                .map(BillMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> getCustomerBills(
            Long customerId,
            Integer month,
            Integer year,
            BillStatus status,
            Pageable pageable) {

        customerService.getCustomerById(customerId);
        return listBills(customerId, null, month, year, status, null, true, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> getCurrentCustomerBills(
            Integer month,
            Integer year,
            BillStatus status,
            Pageable pageable) {

        Customer customer = resolveCustomerForCurrentUser();
        return listBills(customer.getId(), null, month, year, status, null, true, pageable);
    }

    private Bill findBillWithDetailsOrThrow(Long id) {
        return billRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    private void verifyCustomerBillAccess(Bill bill) {
        Customer currentCustomer = resolveCustomerForCurrentUser();
        if (!currentCustomer.getId().equals(bill.getCustomer().getId())) {
            throw new ForbiddenException("Unauthorized to access this resource.");
        }
        if (!Boolean.TRUE.equals(bill.getApproved())) {
            throw new ForbiddenException("Unauthorized to access this resource.");
        }
    }

    private Customer resolveCustomerForCurrentUser() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        String email = userDetails.getUser().getEmail().trim().toLowerCase();
        return customerService.getCustomerByEmail(email);
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }
        throw new ResourceNotFoundException("Authenticated user not found");
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
