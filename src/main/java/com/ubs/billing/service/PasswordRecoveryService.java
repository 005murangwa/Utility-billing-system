package com.ubs.billing.service;

import com.ubs.billing.dto.request.ForgotPasswordRequest;
import com.ubs.billing.dto.request.ResetPasswordRequest;
import com.ubs.billing.dto.request.VerifyPasswordResetOtpRequest;
import com.ubs.billing.dto.response.MessageResponse;
import com.ubs.billing.dto.response.PasswordResetTokenResponse;
import com.ubs.billing.entity.OtpType;
import com.ubs.billing.entity.PasswordResetToken;
import com.ubs.billing.entity.User;
import com.ubs.billing.exception.BadRequestException;
import com.ubs.billing.exception.ResourceNotFoundException;
import com.ubs.billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final String FORGOT_PASSWORD_MESSAGE =
            "If an account exists with this email, a password reset OTP has been sent.";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                log.warn("Password reset requested for disabled account: {}", email);
                return;
            }
            otpService.createAndSendOtp(user, OtpType.PASSWORD_RESET);
        });

        return MessageResponse.builder()
                .message(FORGOT_PASSWORD_MESSAGE)
                .build();
    }

    @Transactional
    public PasswordResetTokenResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Account is disabled");
        }

        otpService.verifyOtp(user, request.getOtp(), OtpType.PASSWORD_RESET);

        String resetToken = passwordResetTokenService.createResetToken(user);

        return PasswordResetTokenResponse.builder()
                .resetToken(resetToken)
                .expiresInMinutes((long) passwordResetTokenService.getTokenExpirationMinutes())
                .message("OTP verified successfully. Use the reset token to set a new password.")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenService.validateResetToken(request.getResetToken());
        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenService.markTokenUsed(request.getResetToken());
        refreshTokenService.revokeAllUserTokens(user.getId());

        return MessageResponse.builder()
                .message("Password has been reset successfully. Please log in with your new password.")
                .build();
    }
}
