package com.ubs.billing.repository;

import com.ubs.billing.entity.Payment;
import com.ubs.billing.entity.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.bill b
            JOIN FETCH b.customer
            WHERE p.id = :id
            """)
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT p FROM Payment p
            WHERE (:billId IS NULL OR p.bill.id = :billId)
              AND (:customerId IS NULL OR p.bill.customer.id = :customerId)
              AND (:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod)
              AND (:billReference IS NULL OR LOWER(p.bill.billReference) LIKE LOWER(CONCAT('%', :billReference, '%')))
            """)
    Page<Payment> searchPayments(
            @Param("billId") Long billId,
            @Param("customerId") Long customerId,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("billReference") String billReference,
            Pageable pageable);
}
