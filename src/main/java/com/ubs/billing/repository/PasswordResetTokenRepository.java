package com.ubs.billing.repository;

import com.ubs.billing.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken prt SET prt.used = true
            WHERE prt.user.id = :userId AND prt.used = false
            """)
    void invalidateActiveTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.tokenHash = :tokenHash")
    void markUsedByTokenHash(@Param("tokenHash") String tokenHash);
}
