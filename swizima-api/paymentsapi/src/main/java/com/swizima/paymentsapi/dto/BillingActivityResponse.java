package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.BillingActivity;
import com.swizima.paymentsapi.entity.enums.BillingActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillingActivityResponse(
        UUID id,
        UUID walletId,
        UUID clientRef,
        BillingActivityType activityType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        String transactionRef,
        BigDecimal transactionAmount,
        BigDecimal percentageApplied,
        UUID creditLoadId,
        String paymentReference,
        String description,
        LocalDateTime createdAt,
        String createdBy
) {

    public static BillingActivityResponse from(BillingActivity b) {
        return new BillingActivityResponse(
                b.getId(), b.getWallet().getId(), b.getClientRef(), b.getActivityType(),
                b.getAmount(), b.getBalanceBefore(), b.getBalanceAfter(), b.getCurrency(),
                b.getTransactionRef(), b.getTransactionAmount(), b.getPercentageApplied(),
                b.getCreditLoad() == null ? null : b.getCreditLoad().getId(),
                b.getPaymentReference(), b.getDescription(), b.getCreatedAt(), b.getCreatedBy());
    }
}
