package com.swizima.paymentsapi.entity;

import com.swizima.paymentsapi.entity.enums.BillingActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable ledger entry: one row per balance movement (credit load, transaction
 * charge or reversal) with a balance snapshot before and after.
 * The (transaction_ref, activity_type) unique key makes charges idempotent.
 */
@Entity
@Table(name = "billing_activities",
        indexes = {
                @Index(name = "idx_ba_wallet", columnList = "wallet_id"),
                @Index(name = "idx_ba_client_ref", columnList = "client_ref"),
                @Index(name = "idx_ba_created_at", columnList = "created_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_ba_txn_type",
                columnNames = {"transaction_ref", "activity_type"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BillingActivity extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private BillingWallet wallet;

    /** Denormalised for fast per-client queries. */
    @Column(name = "client_ref", nullable = false, updatable = false)
    private UUID clientRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30, updatable = false)
    private BillingActivityType activityType;

    /** Signed amount applied to the balance: positive = credit, negative = debit. */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    // ---- Transaction charge details (null for credit loads) ----

    /** Id of the originating transaction (Client Transactions API). */
    @Column(name = "transaction_ref", length = 100, updatable = false)
    private String transactionRef;

    @Column(name = "transaction_amount", precision = 15, scale = 2, updatable = false)
    private BigDecimal transactionAmount;

    /** Rate in force when the charge was computed. */
    @Column(name = "percentage_applied", precision = 7, scale = 4, updatable = false)
    private BigDecimal percentageApplied;

    // ---- Credit load details ----

    /** Top-up this activity belongs to (CREDIT_LOAD / CREDIT_LOAD_REVERSAL only). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_load_id", updatable = false)
    private CreditLoad creditLoad;

    /** External payment reference (e.g. mobile money / bank receipt). */
    @Column(name = "payment_reference", length = 100, updatable = false)
    private String paymentReference;

    @Column(name = "description", length = 500, updatable = false)
    private String description;
}
