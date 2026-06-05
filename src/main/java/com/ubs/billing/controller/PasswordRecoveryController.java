package com.ubs.billing.controller;

import com.ubs.billing.dto.request.ForgotPasswordRequest;
import com.ubs.billing.dto.request.ResetPasswordRequest;
import com.ubs.billing.dto.request.VerifyPasswordResetOtpRequest;
import com.ubs.billing.dto.response.MessageResponse;
import com.ubs.billing.dto.response.PasswordResetTokenResponse;
import com.ubs.billing.service.PasswordRecoveryService;
import com.ubs.billing.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
@Tag(name = "Password Recovery", description = "Forgot password, OTP verification, and password reset")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/forgot")
    @Operation(
            summary = "Forgot password",
            description = "Generates a password reset OTP and sends it to the registered email. "
                    + "Always returns a generic message to prevent email enumeration."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = passwordRecoveryService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify password reset OTP",
            description = "Validates the OTP sent for password recovery and issues a one-time password reset token"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified, reset token issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PasswordResetTokenResponse>> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {
        PasswordResetTokenResponse response = passwordRecoveryService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Reset password",
            description = "Resets the account password using a valid reset token. "
                    + "Invalidates the token after use and revokes active refresh tokens."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid, expired, or reused token", content = @Content)
    })
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = passwordRecoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
