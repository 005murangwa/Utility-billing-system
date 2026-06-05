package com.ubs.billing.repository;

import com.ubs.billing.entity.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, Long> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("DELETE FROM JwtBlacklist jb WHERE jb.expiresAt < :now")
    int deleteExpiredEntries(@Param("now") LocalDateTime now);
}
