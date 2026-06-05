package com.ubs.billing.dto.response;

import com.ubs.billing.entity.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private Long id;
    private String fullName;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private String address;
    private CustomerStatus status;
    private Boolean canReceiveBills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
