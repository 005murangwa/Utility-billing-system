package com.ubs.billing.dto.request;

import com.ubs.billing.validation.LowercaseEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Verify password reset OTP request")
public class VerifyPasswordResetOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @LowercaseEmail
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit code")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}
