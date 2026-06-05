package com.ubs.billing.dto.request;

import com.ubs.billing.validation.LowercaseEmail;
import com.ubs.billing.validation.RwandaPhone;
import com.ubs.billing.validation.StrongPassword;
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
@Schema(description = "User registration request")
public class RegisterRequest {

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

    @NotBlank(message = "Password is required")
    @StrongPassword
    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Optional. Auto-generated from email when omitted")
    private String username;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Schema(hidden = true)
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Schema(hidden = true)
    private String lastName;
}
