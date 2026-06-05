package com.ubs.billing.repository;

import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByMeterIdAndMonthAndYear(Long meterId, Integer month, Integer year);

    long countByMonthAndYear(Integer month, Integer year);

    long countByCustomerId(Long customerId);

    @Query("""
            SELECT b FROM Bill b
            JOIN FETCH b.customer
            JOIN FETCH b.meter
            JOIN FETCH b.tariff
            WHERE b.approved = TRUE
              AND b.balance > 0
              AND b.dueDate < :today
              AND b.status IN :statuses
            """)
    List<Bill> findOverdueCandidates(
            @Param("today") LocalDate today,
            @Param("statuses") Collection<BillStatus> statuses);

    Optional<Bill> findByBillReference(String billReference);

    @Query("""
            SELECT b FROM Bill b
            JOIN FETCH b.customer
            JOIN FETCH b.meter
            JOIN FETCH b.tariff
            JOIN FETCH b.meterReading
            WHERE b.id = :id
            """)
    Optional<Bill> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT b FROM Bill b
            WHERE (:customerId IS NULL OR b.customer.id = :customerId)
              AND (:meterId IS NULL OR b.meter.id = :meterId)
              AND (:month IS NULL OR b.month = :month)
              AND (:year IS NULL OR b.year = :year)
              AND (:status IS NULL OR b.status = :status)
              AND (:billReference = '' OR LOWER(b.billReference) LIKE LOWER(CONCAT('%', :billReference, '%')))
              AND (:approved IS NULL OR b.approved = :approved)
            """)
    Page<Bill> searchBills(
            @Param("customerId") Long customerId,
            @Param("meterId") Long meterId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("status") BillStatus status,
            @Param("billReference") String billReference,
            @Param("approved") Boolean approved,
            Pageable pageable);
}
