package com.ubs.billing.service;

import com.ubs.billing.entity.PasswordResetToken;
import com.ubs.billing.entity.User;
import com.ubs.billing.exception.InvalidResetTokenException;
import com.ubs.billing.exception.ResetTokenExpiredException;
import com.ubs.billing.repository.PasswordResetTokenRepository;
import com.ubs.billing.security.PasswordResetProperties;
import com.ubs.billing.util.TokenHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetProperties passwordResetProperties;

    @Transactional
    public String createResetToken(User user) {
        passwordResetTokenRepository.invalidateActiveTokensByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(TokenHashUtils.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenExpirationMinutes()))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateResetToken(String rawToken) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(TokenHashUtils.sha256(rawToken))
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or already used password reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new InvalidResetTokenException("Invalid or already used password reset token");
        }

        if (resetToken.isExpired()) {
            throw new ResetTokenExpiredException("Password reset token has expired. Please restart the recovery process.");
        }

        return resetToken;
    }

    @Transactional
    public void markTokenUsed(String rawToken) {
        passwordResetTokenRepository.markUsedByTokenHash(TokenHashUtils.sha256(rawToken));
    }

    public int getTokenExpirationMinutes() {
        return passwordResetProperties.getTokenExpirationMinutes();
    }
}
