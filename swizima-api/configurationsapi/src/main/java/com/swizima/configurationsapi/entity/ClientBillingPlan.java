package com.swizima.configurationsapi.entity;

import com.swizima.configurationsapi.entity.enums.BillingModel;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Billing rules for a client: either a percentage charged per transaction (COMMISSION)
 * or an agreed fixed amount paid at the end of every month (MONTHLY_FLAT).
 * The money side (wallet balance, credit loads, charges) lives in the Payments API,
 * which reads this plan when charging a transaction.
 */
@Entity
@Table(name = "client_billing_plans",
        uniqueConstraints = @UniqueConstraint(name = "uk_cbp_client_ref", columnNames = "client_ref"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ClientBillingPlan extends ParentEntity {

    /** Id of the client in the Client API. */
    @Column(name = "client_ref", nullable = false, updatable = false)
    private UUID clientRef;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_model", length = 20)
    private BillingModel billingModel = BillingModel.COMMISSION;

    /** Fee charged per transaction, as a percentage of the transaction amount (e.g. 5 = 5%). 0 for MONTHLY_FLAT. */
    @Column(name = "percentage_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal percentageRate;

    /** Agreed fixed monthly amount payable at the end of the month (used when billingModel == MONTHLY_FLAT). */
    @Column(name = "monthly_amount", precision = 15, scale = 2)
    private BigDecimal monthlyAmount;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "UGX";

    /** Wallet balance at/below which the client is flagged as low on credit. */
    @Column(name = "low_balance_threshold", precision = 15, scale = 2)
    private BigDecimal lowBalanceThreshold;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillingPlanStatus status = BillingPlanStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;
}
