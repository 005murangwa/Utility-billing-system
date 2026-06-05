package com.ubs.billing.repository;

import com.ubs.billing.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByEmail(String email);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:fullName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')))
              AND (:nationalId IS NULL OR LOWER(c.nationalId) LIKE LOWER(CONCAT('%', :nationalId, '%')))
              AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))
            """)
    Page<Customer> searchCustomers(
            @Param("fullName") String fullName,
            @Param("nationalId") String nationalId,
            @Param("email") String email,
            Pageable pageable);
}
