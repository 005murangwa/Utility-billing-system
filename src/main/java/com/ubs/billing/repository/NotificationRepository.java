package com.ubs.billing.repository;

import com.ubs.billing.entity.Notification;
import com.ubs.billing.entity.NotificationEventType;
import com.ubs.billing.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.customer
            WHERE n.id = :id
            """)
    Optional<Notification> findByIdWithCustomer(@Param("id") Long id);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.customer.id = :customerId
              AND (:status IS NULL OR n.status = :status)
            """)
    Page<Notification> findByCustomerId(
            @Param("customerId") Long customerId,
            @Param("status") NotificationStatus status,
            Pageable pageable);

    boolean existsByCustomerIdAndEventTypeAndReferenceId(
            Long customerId,
            NotificationEventType eventType,
            Long referenceId);
}
