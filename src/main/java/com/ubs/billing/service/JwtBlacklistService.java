package com.ubs.billing.service;

import com.ubs.billing.entity.JwtBlacklist;
import com.ubs.billing.repository.JwtBlacklistRepository;
import com.ubs.billing.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final JwtBlacklistRepository jwtBlacklistRepository;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        String jti = jwtService.extractJti(token);
        if (jti == null) {
            return false;
        }
        return jwtBlacklistRepository.existsByJti(jti);
    }

    @Transactional
    public void blacklistToken(String token) {
        String jti = jwtService.extractJti(token);
        Date expiration = jwtService.extractExpiration(token);
        if (jti == null || expiration == null) {
            return;
        }

        if (jwtBlacklistRepository.existsByJti(jti)) {
            return;
        }

        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault());
        JwtBlacklist blacklistEntry = JwtBlacklist.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .build();
        jwtBlacklistRepository.save(blacklistEntry);
    }

    @Transactional
    public void purgeExpiredEntries() {
        jwtBlacklistRepository.deleteExpiredEntries(LocalDateTime.now());
    }
}
