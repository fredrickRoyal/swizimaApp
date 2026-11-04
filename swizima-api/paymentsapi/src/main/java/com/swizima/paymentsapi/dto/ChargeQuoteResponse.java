package com.swizima.paymentsapi.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Preview of the fee for a transaction amount, without deducting anything. */
public record ChargeQuoteResponse(
        UUID clientRef,
        BigDecimal transactionAmount,
        BigDecimal percentageRate,
        BigDecimal chargeAmount,
        String currency,
        BigDecimal currentBalance,
        boolean sufficientBalance
) {
}
