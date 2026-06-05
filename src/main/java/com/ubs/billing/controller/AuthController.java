package com.ubs.billing.controller;

import com.ubs.billing.dto.request.ChangeTemporaryPasswordRequest;
import com.ubs.billing.dto.request.LoginRequest;
import com.ubs.billing.dto.request.LogoutRequest;
import com.ubs.billing.dto.request.RefreshTokenRequest;
import com.ubs.billing.dto.request.RegisterRequest;
import com.ubs.billing.dto.request.ResendOtpRequest;
import com.ubs.billing.dto.request.VerifyOtpRequest;
import com.ubs.billing.dto.response.AuthResponse;
import com.ubs.billing.dto.response.MessageResponse;
import com.ubs.billing.dto.response.RegisterResponse;
import com.ubs.billing.service.AuthService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, email verification, login, refresh, and logout")
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account", description = "Creates an inactive account and sends an email verification OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone already exists")
    })
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify email OTP", description = "Verifies the OTP sent to email and activates the account")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        MessageResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend email verification OTP", description = "Invalidates previous OTP and sends a new verification code")
    public ResponseEntity<ApiResponse<MessageResponse>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        MessageResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/change-temporary-password")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
            summary = "Change temporary password",
            description = "Required on first login for admin-created staff accounts. Returns new tokens with passwordChangeRequired=false."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> changeTemporaryPassword(
            @Valid @RequestBody ChangeTemporaryPasswordRequest request) {
        AuthResponse response = authService.changeTemporaryPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Authenticate with email and password. Staff accounts with temporary passwords receive passwordChangeRequired=true."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Email not verified", content = @Content)
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Logout", description = "Blacklists the current access token and optionally revokes the refresh token")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request) {
        String accessToken = extractBearerToken(authorizationHeader);
        MessageResponse response = authService.logout(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
