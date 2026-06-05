package com.ubs.billing.repository;

import com.ubs.billing.entity.MeterType;
import com.ubs.billing.entity.Tariff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM Tariff t WHERE t.meterType = :meterType")
    Integer findMaxVersionByMeterType(@Param("meterType") MeterType meterType);

    @Query("""
            SELECT t FROM Tariff t
            WHERE (:meterType IS NULL OR t.meterType = :meterType)
              AND (:active IS NULL OR t.active = :active)
            """)
    Page<Tariff> searchTariffs(
            @Param("meterType") MeterType meterType,
            @Param("active") Boolean active,
            Pageable pageable);

    Optional<Tariff> findFirstByMeterTypeAndActiveTrueAndEffectiveDateLessThanEqualOrderByVersionDescEffectiveDateDesc(
            MeterType meterType,
            LocalDate billingDate);

    @Modifying
    @Query("""
            UPDATE Tariff t SET t.active = false
            WHERE t.meterType = :meterType AND t.active = true
            """)
    void deactivateAllActiveByMeterType(@Param("meterType") MeterType meterType);
}
