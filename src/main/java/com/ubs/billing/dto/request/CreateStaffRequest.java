package com.ubs.billing.dto.request;

import com.ubs.billing.validation.LowercaseEmail;
import com.ubs.billing.validation.RwandaPhone;
import com.ubs.billing.validation.ValidStaffRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Admin request to create a staff account with a temporary password")
public class CreateStaffRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @LowercaseEmail
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @RwandaPhone
    private String phoneNumber;

    @NotBlank(message = "Role is required")
    @ValidStaffRole
    @Schema(description = "Staff role: ROLE_OPERATOR, ROLE_FINANCE, or ROLE_ADMIN", example = "ROLE_OPERATOR")
    private String role;
}
