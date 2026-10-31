package com.swizima.clienttransactionsapi.entity;

import com.swizima.clienttransactionsapi.entity.enums.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payment (full or partial) made against a vehicle service transaction. */
@Entity
@Table(name = "transaction_payments", indexes = {
        @Index(name = "idx_tp_transaction", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TransactionPayment extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private VehicleServiceTransaction transaction;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "paid_by_name", length = 150)
    private String paidByName;

    @Column(name = "paid_by_phone", length = 30)
    private String paidByPhone;

    @Column(name = "received_by", length = 150)
    private String receivedBy;

    @Column(name = "payment_date_time", nullable = false)
    private LocalDateTime paymentDateTime;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
}
