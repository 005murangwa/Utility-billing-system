package com.ubs.billing.repository;

import com.ubs.billing.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    boolean existsByMeterIdAndMonthAndYear(Long meterId, Integer month, Integer year);

    boolean existsByMeterIdAndMonthAndYearAndIdNot(Long meterId, Integer month, Integer year, Long id);

    @Query("""
            SELECT mr FROM MeterReading mr
            JOIN FETCH mr.meter m
            JOIN FETCH m.customer
            WHERE mr.id = :id
            """)
    Optional<MeterReading> findByIdWithMeter(@Param("id") Long id);

    @Query("""
            SELECT mr FROM MeterReading mr
            WHERE (:meterId IS NULL OR mr.meter.id = :meterId)
              AND (:month IS NULL OR mr.month = :month)
              AND (:year IS NULL OR mr.year = :year)
              AND (:meterNumber IS NULL OR LOWER(mr.meter.meterNumber) LIKE LOWER(CONCAT('%', :meterNumber, '%')))
            """)
    Page<MeterReading> searchReadings(
            @Param("meterId") Long meterId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("meterNumber") String meterNumber,
            Pageable pageable);
}
