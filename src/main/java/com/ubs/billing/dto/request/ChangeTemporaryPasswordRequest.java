package com.ubs.billing.dto.request;

import com.ubs.billing.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "First-login password change request")
public class ChangeTemporaryPasswordRequest {

    @NotBlank(message = "Current password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
