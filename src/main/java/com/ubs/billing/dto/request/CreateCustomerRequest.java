package com.ubs.billing.dto.request;

import com.ubs.billing.entity.CustomerStatus;
import com.ubs.billing.validation.LowercaseEmail;
import com.ubs.billing.validation.RwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create customer request")
public class CreateCustomerRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^\\d{16}$", message = "National ID must be exactly 16 digits")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @LowercaseEmail
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @RwandaPhone
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Status is required")
    private CustomerStatus status;
}
