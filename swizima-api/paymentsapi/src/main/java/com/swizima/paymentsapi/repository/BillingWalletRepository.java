package com.swizima.paymentsapi.repository;

import com.swizima.paymentsapi.entity.BillingWallet;
import com.swizima.paymentsapi.entity.enums.RecordStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingWalletRepository extends JpaRepository<BillingWallet, UUID> {

    Optional<BillingWallet> findByClientRefAndRecordStatusNot(UUID clientRef, RecordStatus recordStatus);

    /** Row-locks the wallet so concurrent balance changes are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM BillingWallet w WHERE w.clientRef = :clientRef AND w.recordStatus <> :excluded")
    Optional<BillingWallet> lockByClientRef(@Param("clientRef") UUID clientRef,
                                            @Param("excluded") RecordStatus excluded);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM BillingWallet w WHERE w.id = :id")
    Optional<BillingWallet> lockById(@Param("id") UUID id);

    /**
     * Creates the wallet if it doesn't exist yet. Safe under concurrency: two first-time
     * requests for the same client can't create two wallets.
     */
    @Modifying
    @Query(value = """
            INSERT INTO billing_wallets (id, client_ref, currency, balance, total_credited, total_charged,
                                         version, record_status, created_at, created_by)
            VALUES (gen_random_uuid(), :clientRef, :currency, 0, 0, 0, 0, 'ACTIVE', now(), 'system')
            ON CONFLICT (client_ref) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("clientRef") UUID clientRef, @Param("currency") String currency);

    @Query("""
            SELECT w FROM BillingWallet w
            WHERE w.recordStatus <> :excluded
              AND (:maxBalance IS NULL OR w.balance <= :maxBalance)
            """)
    Page<BillingWallet> search(@Param("maxBalance") BigDecimal maxBalance,
                               @Param("excluded") RecordStatus excluded,
                               Pageable pageable);
}
