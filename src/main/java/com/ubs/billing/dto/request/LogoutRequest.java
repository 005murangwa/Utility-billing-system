package com.ubs.billing.dto.request;

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
@Schema(description = "Logout request with optional refresh token revocation")
public class LogoutRequest {

    @Schema(description = "Optional refresh token to revoke during logout")
    private String refreshToken;
}
