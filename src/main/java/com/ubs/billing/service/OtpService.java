package com.ubs.billing.service;

import com.ubs.billing.entity.Otp;
import com.ubs.billing.entity.OtpType;
import com.ubs.billing.entity.User;
import com.ubs.billing.exception.InvalidOtpException;
import com.ubs.billing.exception.OtpExpiredException;
import com.ubs.billing.repository.OtpRepository;
import com.ubs.billing.security.OtpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;
    private final EmailService emailService;

    @Transactional
    public void createAndSendOtp(User user, OtpType otpType) {
        otpRepository.invalidateActiveOtps(user.getId(), otpType);

        String otpCode = generateOtpCode();
        Otp otp = Otp.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(otpCode))
                .otpType(otpType)
                .expiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()))
                .used(false)
                .build();
        otpRepository.save(otp);

        switch (otpType) {
            case EMAIL_VERIFICATION -> emailService.sendOtpEmail(user, otpCode);
            case PASSWORD_RESET -> emailService.sendPasswordResetOtpEmail(user, otpCode);
        }
    }

    @Transactional
    public void verifyOtp(User user, String rawOtp, OtpType otpType) {
        Otp otp = otpRepository.findLatestValidOtp(user.getId(), otpType, LocalDateTime.now())
                .orElseThrow(() -> new OtpExpiredException("OTP has expired or is invalid. Please request a new one."));

        if (otp.isExpired()) {
            throw new OtpExpiredException("OTP has expired. Please request a new one.");
        }

        if (!passwordEncoder.matches(rawOtp, otp.getCodeHash())) {
            throw new InvalidOtpException("Invalid OTP code");
        }

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    public int getExpirationMinutes() {
        return otpProperties.getExpirationMinutes();
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpProperties.getLength());
        int floor = bound / 10;
        int code = SECURE_RANDOM.nextInt(bound - floor) + floor;
        return String.valueOf(code);
    }
}
