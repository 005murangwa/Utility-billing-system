package com.ubs.billing.repository;

import com.ubs.billing.entity.Otp;
import com.ubs.billing.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Query("""
            SELECT o FROM Otp o
            WHERE o.user.id = :userId
              AND o.otpType = :otpType
              AND o.used = false
              AND o.expiresAt > :now
            ORDER BY o.createdAt DESC
            """)
    Optional<Otp> findLatestValidOtp(
            @Param("userId") Long userId,
            @Param("otpType") OtpType otpType,
            @Param("now") LocalDateTime now);

    List<Otp> findByUserIdAndOtpTypeAndUsedFalse(Long userId, OtpType otpType);

    @Modifying
    @Query("""
            UPDATE Otp o SET o.used = true
            WHERE o.user.id = :userId AND o.otpType = :otpType AND o.used = false
            """)
    void invalidateActiveOtps(@Param("userId") Long userId, @Param("otpType") OtpType otpType);
}
