package com.swizima.paymentsapi.repository;

import com.swizima.paymentsapi.entity.BillingActivity;
import com.swizima.paymentsapi.entity.enums.BillingActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingActivityRepository extends JpaRepository<BillingActivity, UUID> {

    Optional<BillingActivity> findByTransactionRefAndActivityType(String transactionRef, BillingActivityType type);

    /** Any filter passed as null is ignored. */
    @Query("""
            SELECT b FROM BillingActivity b
            WHERE (:clientRef      IS NULL OR b.clientRef = :clientRef)
              AND (:type           IS NULL OR b.activityType = :type)
              AND (:transactionRef IS NULL OR b.transactionRef = :transactionRef)
              AND (CAST(:from AS timestamp) IS NULL OR b.createdAt >= :from)
              AND (CAST(:to   AS timestamp) IS NULL OR b.createdAt <  :to)
            """)
    Page<BillingActivity> search(@Param("clientRef") UUID clientRef,
                                 @Param("type") BillingActivityType type,
                                 @Param("transactionRef") String transactionRef,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 Pageable pageable);
}
