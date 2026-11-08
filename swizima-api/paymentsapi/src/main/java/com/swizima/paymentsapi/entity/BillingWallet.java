package com.swizima.paymentsapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A client's prepaid billing credit. The charge percentage is NOT stored here; it is
 * read from the client's billing plan in the Configurations API at charge time.
 * Every balance change is recorded as a {@link BillingActivity}.
 * Created automatically on the client's first credit load or charge.
 */
@Entity
@Table(name = "billing_wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_bw_client_ref", columnNames = "client_ref"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BillingWallet extends ParentEntity {

    /** Id of the client in the Client API. */
    @Column(name = "client_ref", nullable = false, updatable = false)
    private UUID clientRef;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "UGX";

    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Lifetime total of credit loaded (net of reversed loads). */
    @Builder.Default
    @Column(name = "total_credited", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCredited = BigDecimal.ZERO;

    /** Lifetime total of net charges (charges minus reversals). */
    @Builder.Default
    @Column(name = "total_charged", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCharged = BigDecimal.ZERO;

    /** Optimistic lock as a second line of defence; balance updates also take a row lock. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
