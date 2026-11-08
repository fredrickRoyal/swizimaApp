package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.BillingWallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet balance, optionally enriched with the client's billing plan
 * (billing model / rate / monthly amount / low-balance threshold from the Configurations API).
 */
public record BillingWalletResponse(
        UUID id,
        UUID clientRef,
        String currency,
        BigDecimal balance,
        BigDecimal totalCredited,
        BigDecimal totalCharged,
        String billingModel,
        BigDecimal percentageRate,
        BigDecimal monthlyAmount,
        BigDecimal lowBalanceThreshold,
        Boolean lowBalance,
        String planStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BillingWalletResponse from(BillingWallet w) {
        return from(w, null);
    }

    public static BillingWalletResponse from(BillingWallet w, BillingPlan plan) {
        BigDecimal threshold = plan == null ? null : plan.lowBalanceThreshold();
        Boolean low = (plan != null && plan.isMonthlyFlat())
                ? Boolean.FALSE
                : (threshold == null ? null : w.getBalance().compareTo(threshold) <= 0);
        return new BillingWalletResponse(
                w.getId(),
                w.getClientRef(),
                w.getCurrency(),
                w.getBalance(),
                w.getTotalCredited(),
                w.getTotalCharged(),
                plan == null ? null : (plan.billingModel() != null ? plan.billingModel() : "COMMISSION"),
                plan == null ? null : plan.percentageRate(),
                plan == null ? null : plan.monthlyAmount(),
                threshold,
                low,
                plan == null ? null : plan.status(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
