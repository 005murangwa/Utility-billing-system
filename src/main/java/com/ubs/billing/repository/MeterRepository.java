package com.ubs.billing.repository;

import com.ubs.billing.entity.Meter;
import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    long countByCustomerId(Long customerId);

    boolean existsByMeterNumberAndIdNot(String meterNumber, Long id);

    Optional<Meter> findByMeterNumber(String meterNumber);

    @Query("""
            SELECT m FROM Meter m
            JOIN FETCH m.customer
            WHERE m.id = :id
            """)
    Optional<Meter> findByIdWithCustomer(@Param("id") Long id);

    @Query("""
            SELECT m FROM Meter m
            WHERE (:meterNumber = '' OR LOWER(m.meterNumber) LIKE LOWER(CONCAT('%', :meterNumber, '%')))
              AND (:meterType IS NULL OR m.meterType = :meterType)
              AND (:status IS NULL OR m.status = :status)
              AND (:customerId IS NULL OR m.customer.id = :customerId)
            """)
    Page<Meter> searchMeters(
            @Param("meterNumber") String meterNumber,
            @Param("meterType") MeterType meterType,
            @Param("status") MeterStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable);

    @Query("""
            SELECT m FROM Meter m
            WHERE m.customer.id = :customerId
              AND (:meterNumber = '' OR LOWER(m.meterNumber) LIKE LOWER(CONCAT('%', :meterNumber, '%')))
              AND (:meterType IS NULL OR m.meterType = :meterType)
              AND (:status IS NULL OR m.status = :status)
            """)
    Page<Meter> findByCustomerId(
            @Param("customerId") Long customerId,
            @Param("meterNumber") String meterNumber,
            @Param("meterType") MeterType meterType,
            @Param("status") MeterStatus status,
            Pageable pageable);
}
