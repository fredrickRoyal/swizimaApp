package com.swizima.paymentsapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Client billing plan as returned by the Configurations API (only the fields Payments needs). */
public record BillingPlan(
        UUID clientRef,
        BigDecimal percentageRate,
        String currency,
        BigDecimal lowBalanceThreshold,
        String status
) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
