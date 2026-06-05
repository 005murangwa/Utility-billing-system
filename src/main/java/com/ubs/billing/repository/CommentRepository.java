package com.ubs.billing.repository;

import com.ubs.billing.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.user
            JOIN FETCH c.bill b
            JOIN FETCH b.customer
            WHERE c.id = :id
            """)
    Optional<Comment> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT c FROM Comment c
            WHERE c.bill.id = :billId
              AND (:search IS NULL OR LOWER(c.comment) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.user.username) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Comment> findByBillId(
            @Param("billId") Long billId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:billId IS NULL OR c.bill.id = :billId)
              AND (:customerId IS NULL OR c.bill.customer.id = :customerId)
              AND (:search IS NULL OR LOWER(c.comment) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.user.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.bill.billReference) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Comment> searchComments(
            @Param("billId") Long billId,
            @Param("customerId") Long customerId,
            @Param("search") String search,
            Pageable pageable);
}
