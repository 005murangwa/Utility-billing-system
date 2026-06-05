package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.CommentMapper;
import com.ubs.billing.dto.request.CreateCommentRequest;
import com.ubs.billing.dto.response.CommentResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.Comment;
import com.ubs.billing.exception.ForbiddenException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.BillRepository;
import com.ubs.billing.repository.CommentRepository;
import com.ubs.billing.repository.CustomerRepository;
import com.ubs.billing.security.CustomUserDetails;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {
        CustomUserDetails userDetails = requireAuthenticatedUser();
        Bill bill = billRepository.findByIdWithDetails(request.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + request.getBillId()));

        verifyBillAccess(bill, userDetails);

        Comment comment = Comment.builder()
                .user(userDetails.getUser())
                .bill(bill)
                .comment(request.getComment().trim())
                .build();

        Comment savedComment = commentRepository.save(comment);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.COMMENT, savedComment.getId());

        return CommentMapper.toResponse(
                commentRepository.findByIdWithDetails(savedComment.getId()).orElse(savedComment));
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        verifyBillAccess(comment.getBill(), requireAuthenticatedUser());
        return CommentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(
            Long billId,
            Long customerId,
            String search,
            Pageable pageable) {

        CustomUserDetails userDetails = requireAuthenticatedUser();
        Long scopedCustomerId = resolveCustomerIdForQuery(userDetails, customerId);

        Page<CommentResponse> page = commentRepository
                .searchComments(billId, scopedCustomerId, normalizeSearchParam(search), pageable)
                .map(comment -> {
                    if (isCustomerOnly(userDetails)) {
                        verifyBillAccess(comment.getBill(), userDetails);
                    }
                    return CommentMapper.toResponse(comment);
                });

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getBillComments(Long billId, String search, Pageable pageable) {
        Bill bill = billRepository.findByIdWithDetails(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + billId));

        CustomUserDetails userDetails = requireAuthenticatedUser();
        verifyBillAccess(bill, userDetails);

        Page<CommentResponse> page = commentRepository
                .findByBillId(billId, normalizeSearchParam(search), pageable)
                .map(CommentMapper::toResponse);

        return PageResponse.from(page);
    }

    private void verifyBillAccess(Bill bill, CustomUserDetails userDetails) {
        if (!isCustomerOnly(userDetails)) {
            return;
        }

        String userEmail = userDetails.getUser().getEmail().trim().toLowerCase();
        if (!bill.getCustomer().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Unauthorized to access this resource.");
        }
    }

    private Long resolveCustomerIdForQuery(CustomUserDetails userDetails, Long customerId) {
        if (!isCustomerOnly(userDetails)) {
            return customerId;
        }

        String userEmail = userDetails.getUser().getEmail().trim().toLowerCase();
        Long scopedCustomerId = customerRepository.findByEmail(userEmail)
                .map(customer -> customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for the authenticated user"));

        if (customerId != null && !customerId.equals(scopedCustomerId)) {
            throw new ForbiddenException("Unauthorized to access this resource.");
        }

        return scopedCustomerId;
    }

    private boolean isCustomerOnly(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_CUSTOMER".equals(auth.getAuthority()))
                && userDetails.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().startsWith("ROLE_ADMIN")
                        || auth.getAuthority().startsWith("ROLE_FINANCE")
                        || auth.getAuthority().startsWith("ROLE_OPERATOR"));
    }

    private CustomUserDetails requireAuthenticatedUser() {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userDetails;
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
