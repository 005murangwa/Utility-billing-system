package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.PaymentMapper;
import com.ubs.billing.dto.request.CreatePaymentRequest;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.Payment;
import com.ubs.billing.entity.PaymentMethod;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.security.CustomUserDetails;
import com.ubs.billing.repository.BillRepository;
import com.ubs.billing.repository.PaymentRepository;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.util.BillPaymentUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Set<BillStatus> PAYABLE_STATUSES = EnumSet.of(
            BillStatus.APPROVED,
            BillStatus.PARTIALLY_PAID,
            BillStatus.OVERDUE
    );

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final CustomerService customerService;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Bill bill = billRepository.findByIdWithDetails(request.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + request.getBillId()));

        validateBillForPayment(bill, request.getAmountPaid());

        Payment payment = Payment.builder()
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .build();

        BillPaymentUpdater.applyPayment(bill, request.getAmountPaid());

        billRepository.save(bill);
        Payment savedPayment = paymentRepository.save(payment);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.PAYMENT, savedPayment.getId());

        return PaymentMapper.toResponse(
                paymentRepository.findByIdWithDetails(savedPayment.getId())
                        .orElse(savedPayment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        return PaymentMapper.toResponse(findPaymentWithDetailsOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getPayments(
            Long billId,
            Long customerId,
            PaymentMethod paymentMethod,
            String billReference,
            Pageable pageable) {

        Page<PaymentResponse> page = paymentRepository
                .searchPayments(billId, customerId, paymentMethod, normalizePaymentSearchParam(billReference), pageable)
                .map(payment -> {
                    payment.getBill().getBillReference();
                    payment.getBill().getCustomer().getFullName();
                    return PaymentMapper.toResponse(payment);
                });

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getCustomerPaymentHistory(Long customerId, Pageable pageable) {
        customerService.getCustomerById(customerId);
        return getPayments(null, customerId, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getCurrentCustomerPayments(Pageable pageable) {
        Customer customer = resolveCustomerForCurrentUser();
        return getPayments(null, customer.getId(), null, null, pageable);
    }

    private Customer resolveCustomerForCurrentUser() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        return customerService.getCustomerByEmail(userDetails.getUser().getEmail());
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }
        throw new ResourceNotFoundException("Authenticated user not found");
    }

    private void validateBillForPayment(Bill bill, BigDecimal amountPaid) {
        if (!Boolean.TRUE.equals(bill.getApproved())) {
            throw new BadRequestException("Payments can only be made against approved bills");
        }

        if (!PAYABLE_STATUSES.contains(bill.getStatus())) {
            throw new BadRequestException("Bill is not in a payable status");
        }

        if (bill.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Bill has no outstanding balance");
        }

        if (amountPaid.compareTo(bill.getBalance()) > 0) {
            throw new BadRequestException("Payment amount exceeds outstanding bill balance");
        }
    }

    private Payment findPaymentWithDetailsOrThrow(Long id) {
        return paymentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizePaymentSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
