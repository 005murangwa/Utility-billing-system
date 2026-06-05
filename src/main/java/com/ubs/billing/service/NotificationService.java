package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.NotificationMapper;
import com.ubs.billing.dto.response.NotificationResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.Notification;
import com.ubs.billing.entity.NotificationEventType;
import com.ubs.billing.entity.NotificationStatus;
import com.ubs.billing.exception.ForbiddenException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.CustomerRepository;
import com.ubs.billing.repository.NotificationRepository;
import com.ubs.billing.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void createNotificationIfAbsent(
            Customer customer,
            String message,
            NotificationEventType eventType,
            Long referenceId) {

        if (notificationRepository.existsByCustomerIdAndEventTypeAndReferenceId(
                customer.getId(), eventType, referenceId)) {
            return;
        }

        Notification notification = Notification.builder()
                .customer(customer)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .eventType(eventType)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(NotificationStatus status, Pageable pageable) {
        Customer customer = resolveCustomerForCurrentUser();
        return getCustomerNotifications(customer.getId(), status, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getCustomerNotifications(
            Long customerId,
            NotificationStatus status,
            Pageable pageable) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        verifyCustomerOwnership(customer);

        Page<NotificationResponse> page = notificationRepository
                .findByCustomerId(customerId, status, pageable)
                .map(NotificationMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        verifyCustomerOwnership(notification.getCustomer());
        return NotificationMapper.toResponse(notification);
    }

    private void verifyCustomerOwnership(Customer customer) {
        Customer currentCustomer = resolveCustomerForCurrentUser();
        if (!currentCustomer.getId().equals(customer.getId())) {
            throw new ForbiddenException("Unauthorized to access this resource.");
        }
    }

    private Customer resolveCustomerForCurrentUser() {
        CustomUserDetails userDetails = getAuthenticatedUserDetails();
        String email = userDetails.getUser().getEmail().trim().toLowerCase();

        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for the authenticated user"));
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }
        throw new ResourceNotFoundException("Authenticated user not found");
    }
}
