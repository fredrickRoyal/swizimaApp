package com.swizima.paymentsapi.entity;

import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A prepaid credit top-up for a client's billing wallet (e.g. 50,000 UGX via mobile money).
 * Records the payment details; the resulting balance change is the linked
 * {@link BillingActivity} of type CREDIT_LOAD (and CREDIT_LOAD_REVERSAL if reversed).
 */
@Entity
@Table(name = "credit_loads",
        indexes = {
                @Index(name = "idx_cl_wallet", columnList = "wallet_id"),
                @Index(name = "idx_cl_client_ref", columnList = "client_ref"),
                @Index(name = "idx_cl_loaded_at", columnList = "loaded_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_cl_method_reference",
                columnNames = {"payment_method", "payment_reference"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreditLoad extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private BillingWallet wallet;

    @Column(name = "client_ref", nullable = false, updatable = false)
    private UUID clientRef;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    // ---- Payment ----
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20, updatable = false)
    private CreditPaymentMethod paymentMethod;

    /** Receipt / mobile money / bank reference. Unique per payment method to prevent double loading. */
    @Column(name = "payment_reference", length = 100, updatable = false)
    private String paymentReference;

    @Column(name = "payer_name", length = 150, updatable = false)
    private String payerName;

    @Column(name = "payer_phone", length = 30, updatable = false)
    private String payerPhone;

    /** Staff member who received the payment / loaded the credit. */
    @Column(name = "received_by", length = 150, updatable = false)
    private String receivedBy;

    /** When the payment was made (defaults to the time of loading). */
    @Column(name = "loaded_at", nullable = false, updatable = false)
    private LocalDateTime loadedAt;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    // ---- Status / reversal ----
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditLoadStatus status = CreditLoadStatus.COMPLETED;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by", length = 150)
    private String reversedBy;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;
}
