package com.ubs.billing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Password reset token issued after OTP verification")
public class PasswordResetTokenResponse {

    private String resetToken;
    private Long expiresInMinutes;
    private String message;
}
