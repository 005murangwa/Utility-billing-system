package com.ubs.billing.dto.mapper;

import com.ubs.billing.dto.response.CustomerResponse;
import com.ubs.billing.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .canReceiveBills(customer.canReceiveBills())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
