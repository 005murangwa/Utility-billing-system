package com.ubs.billing.dto.request;

import com.ubs.billing.validation.LowercaseEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Resend OTP request")
public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @LowercaseEmail
    private String email;
}
