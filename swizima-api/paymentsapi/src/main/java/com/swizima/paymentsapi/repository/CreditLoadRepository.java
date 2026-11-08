package com.swizima.paymentsapi.repository;

import com.swizima.paymentsapi.entity.CreditLoad;
import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditLoadRepository extends JpaRepository<CreditLoad, UUID> {

    boolean existsByPaymentMethodAndPaymentReferenceIgnoreCase(CreditPaymentMethod method, String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditLoad c WHERE c.id = :id")
    Optional<CreditLoad> lockById(@Param("id") UUID id);

    /** Any filter passed as null is ignored. */
    @Query("""
            SELECT c FROM CreditLoad c
            WHERE (:clientRef IS NULL OR c.clientRef = :clientRef)
              AND (:status    IS NULL OR c.status = :status)
              AND (:method    IS NULL OR c.paymentMethod = :method)
              AND (CAST(:from AS timestamp) IS NULL OR c.loadedAt >= :from)
              AND (CAST(:to   AS timestamp) IS NULL OR c.loadedAt <  :to)
              AND (:search IS NULL
                   OR LOWER(c.paymentReference) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(c.payerName)        LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR c.payerPhone              LIKE CONCAT('%', CAST(:search AS string), '%'))
            """)
    Page<CreditLoad> search(@Param("clientRef") UUID clientRef,
                            @Param("status") CreditLoadStatus status,
                            @Param("method") CreditPaymentMethod method,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            @Param("search") String search,
                            Pageable pageable);

    /** Total completed top-ups for a client in a period (for statements / reports). */
    @Query("""
            SELECT COALESCE(SUM(c.amount), 0) FROM CreditLoad c
            WHERE c.clientRef = :clientRef AND c.status = :status
              AND (CAST(:from AS timestamp) IS NULL OR c.loadedAt >= :from)
              AND (CAST(:to   AS timestamp) IS NULL OR c.loadedAt <  :to)
            """)
    BigDecimal sumAmount(@Param("clientRef") UUID clientRef,
                         @Param("status") CreditLoadStatus status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);
}
