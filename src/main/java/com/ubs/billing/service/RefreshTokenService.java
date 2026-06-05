package com.ubs.billing.service;

import com.ubs.billing.entity.RefreshToken;
import com.ubs.billing.entity.User;
import com.ubs.billing.exception.UnauthorizedException;
import com.ubs.billing.repository.RefreshTokenRepository;
import com.ubs.billing.security.JwtProperties;
import com.ubs.billing.util.TokenHashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtils.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(TokenHashUtils.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        return refreshToken;
    }

    @Transactional
    public void revokeToken(String rawToken) {
        refreshTokenRepository.revokeByTokenHash(TokenHashUtils.sha256(rawToken));
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public long getRefreshExpirationMs() {
        return jwtProperties.getRefreshExpirationMs();
    }
}
