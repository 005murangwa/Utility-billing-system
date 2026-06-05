package com.ubs.billing.dto.request;

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
@Schema(description = "Login request using email (or legacy username for seeded admin)")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Schema(description = "Registered email address or legacy username", example = "user@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @Schema(hidden = true)
    private String username;
}
