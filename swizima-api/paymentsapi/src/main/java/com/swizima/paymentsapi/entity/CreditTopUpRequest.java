package com.swizima.paymentsapi.entity;

import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.entity.enums.CreditRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A credit top-up request submitted by a client (Client Admin / staff) for Super Admin review.
 * When approved, the Super Admin confirms the actual amount to top up and a {@link CreditLoad}
 * is created and linked via {@link #creditLoadId}.
 */
@Entity
@Table(name = "credit_top_up_requests",
        indexes = {
                @Index(name = "idx_ctur_client_ref", columnList = "client_ref"),
                @Index(name = "idx_ctur_status", columnList = "status"),
                @Index(name = "idx_ctur_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreditTopUpRequest extends ParentEntity {

    @Column(name = "client_ref", nullable = false, updatable = false)
    private UUID clientRef;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal requestedAmount;

    /** Confirmed amount actually topped up when approved by Super Admin. */
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private CreditPaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payer_name", length = 150)
    private String payerName;

    @Column(name = "payer_phone", length = 30)
    private String payerPhone;

    @Column(name = "notes", length = 500)
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditRequestStatus status = CreditRequestStatus.PENDING;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 150)
    private String reviewedBy;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    /** ID of the resulting {@link CreditLoad} when approved. */
    @Column(name = "credit_load_id")
    private UUID creditLoadId;
}

