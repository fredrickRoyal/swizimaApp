package com.swizima.paymentsapi.repository;

import com.swizima.paymentsapi.entity.CreditTopUpRequest;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.entity.enums.CreditRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditTopUpRequestRepository extends JpaRepository<CreditTopUpRequest, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CreditTopUpRequest r WHERE r.id = :id")
    Optional<CreditTopUpRequest> lockById(@Param("id") UUID id);

    @Query("""
            SELECT r FROM CreditTopUpRequest r
            WHERE r.recordStatus <> com.swizima.paymentsapi.entity.enums.RecordStatus.DELETED
              AND (:clientRef IS NULL OR r.clientRef = :clientRef)
              AND (:status    IS NULL OR r.status = :status)
              AND (:method    IS NULL OR r.paymentMethod = :method)
              AND (:search IS NULL
                   OR LOWER(r.paymentReference) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(r.payerName)        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR r.payerPhone              LIKE CONCAT('%', CAST(:search AS string), '%'))
            """)
    Page<CreditTopUpRequest> search(@Param("clientRef") UUID clientRef,
                                    @Param("status") CreditRequestStatus status,
                                    @Param("method") CreditPaymentMethod method,
                                    @Param("search") String search,
                                    Pageable pageable);
}

