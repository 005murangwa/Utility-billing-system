package com.ubs.billing.service;

import com.ubs.billing.dto.mapper.CustomerMapper;
import com.ubs.billing.dto.request.CreateCustomerRequest;
import com.ubs.billing.dto.request.UpdateCustomerRequest;
import com.ubs.billing.dto.response.CustomerResponse;
import com.ubs.billing.dto.response.PageResponse;
import com.ubs.billing.entity.AuditAction;
import com.ubs.billing.entity.Customer;
import com.ubs.billing.entity.CustomerStatus;
import com.ubs.billing.exception.ConflictException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.BillRepository;
import com.ubs.billing.repository.CustomerRepository;
import com.ubs.billing.repository.MeterRepository;
import com.ubs.billing.util.AuditEntityNames;
import com.ubs.billing.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final BillRepository billRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phoneNumber = PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber());
        String nationalId = request.getNationalId().trim();

        validateUniqueFields(nationalId, email, phoneNumber, null);

        Customer customer = Customer.builder()
                .fullName(request.getFullName().trim())
                .nationalId(nationalId)
                .email(email)
                .phoneNumber(phoneNumber)
                .address(request.getAddress().trim())
                .status(request.getStatus())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.CUSTOMER, savedCustomer.getId());
        userService.createCustomerUserIfAbsent(
                savedCustomer.getFullName(),
                savedCustomer.getEmail(),
                savedCustomer.getPhoneNumber());
        return CustomerMapper.toResponse(savedCustomer);
    }

    @Transactional
    public void createProfileForSelfRegistration(
            String fullName,
            String nationalId,
            String email,
            String phoneNumber,
            String address) {

        String normalizedEmail = normalizeEmail(email);
        if (customerRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        String normalizedPhone = PhoneUtils.normalizeRwandaPhone(phoneNumber);
        String normalizedNationalId = nationalId.trim();
        validateUniqueFields(normalizedNationalId, normalizedEmail, normalizedPhone, null);

        Customer customer = Customer.builder()
                .fullName(fullName.trim())
                .nationalId(normalizedNationalId)
                .email(normalizedEmail)
                .phoneNumber(normalizedPhone)
                .address(address.trim())
                .status(CustomerStatus.INACTIVE)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        auditLogService.log(AuditAction.CREATE, AuditEntityNames.CUSTOMER, savedCustomer.getId());
    }

    @Transactional
    public void activateProfileForEmail(String email) {
        customerRepository.findByEmail(normalizeEmail(email)).ifPresent(customer -> {
            if (CustomerStatus.ACTIVE != customer.getStatus()) {
                customer.setStatus(CustomerStatus.ACTIVE);
                customerRepository.save(customer);
            }
        });
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findCustomerOrThrow(id);

        String email = normalizeEmail(request.getEmail());
        String phoneNumber = PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber());
        String nationalId = request.getNationalId().trim();

        validateUniqueFields(nationalId, email, phoneNumber, id);

        customer.setFullName(request.getFullName().trim());
        customer.setNationalId(nationalId);
        customer.setEmail(email);
        customer.setPhoneNumber(phoneNumber);
        customer.setAddress(request.getAddress().trim());
        customer.setStatus(request.getStatus());

        Customer updatedCustomer = customerRepository.save(customer);
        auditLogService.log(AuditAction.UPDATE, AuditEntityNames.CUSTOMER, updatedCustomer.getId());
        return CustomerMapper.toResponse(updatedCustomer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = findCustomerOrThrow(id);

        long meterCount = meterRepository.countByCustomerId(id);
        if (meterCount > 0) {
            throw new ConflictException("Cannot delete customer with " + meterCount + " associated meter(s)");
        }

        long billCount = billRepository.countByCustomerId(id);
        if (billCount > 0) {
            throw new ConflictException("Cannot delete customer with " + billCount + " associated bill(s)");
        }

        auditLogService.log(AuditAction.DELETE, AuditEntityNames.CUSTOMER, customer.getId());
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return CustomerMapper.toResponse(findCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for the authenticated user"));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(
            String fullName,
            String nationalId,
            String email,
            Pageable pageable) {

        String searchName = normalizeCustomerSearchParam(fullName);
        String searchNationalId = normalizeCustomerSearchParam(nationalId);
        String searchEmail = normalizeCustomerSearchParam(email);

        if (StringUtils.hasText(searchEmail)) {
            searchEmail = searchEmail.toLowerCase();
        }

        Page<CustomerResponse> page = customerRepository
                .searchCustomers(searchName, searchNationalId, searchEmail, pageable)
                .map(CustomerMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public Customer findActiveCustomerForBilling(Long id) {
        Customer customer = findCustomerOrThrow(id);
        if (!customer.canReceiveBills()) {
            throw new ConflictException("Inactive customers cannot receive bills");
        }
        return customer;
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private void validateUniqueFields(String nationalId, String email, String phoneNumber, Long excludeId) {
        if (excludeId == null) {
            if (customerRepository.existsByNationalId(nationalId)) {
                customerRepository.findByNationalId(nationalId).ifPresent(existing ->
                        throwConflict("National ID " + nationalId + " is already used by customer "
                                + existing.getEmail() + " (id " + existing.getId() + ")"));
                throw new ConflictException("Customer with this national ID already exists");
            }
            if (customerRepository.existsByEmail(email)) {
                customerRepository.findByEmail(email).ifPresent(existing ->
                        throwConflict("Customer profile already exists for " + email
                                + " (id " + existing.getId() + "). No need to create it again."));
                throw new ConflictException("Customer with this email already exists");
            }
            if (customerRepository.existsByPhoneNumber(phoneNumber)) {
                throw new ConflictException("Customer with this phone number already exists");
            }
            return;
        }

        if (customerRepository.existsByNationalIdAndIdNot(nationalId, excludeId)) {
            throw new ConflictException("Customer with this national ID already exists");
        }
        if (customerRepository.existsByEmailAndIdNot(email, excludeId)) {
            throw new ConflictException("Customer with this email already exists");
        }
        if (customerRepository.existsByPhoneNumberAndIdNot(phoneNumber, excludeId)) {
            throw new ConflictException("Customer with this phone number already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCustomerSearchParam(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private void throwConflict(String message) {
        throw new ConflictException(message);
    }
}
