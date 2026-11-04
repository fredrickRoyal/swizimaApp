package com.swizima.paymentsapi.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requests the per-transaction fee deduction. Idempotent on {@code transactionRef}:
 * re-sending the same transaction returns the original charge instead of deducting twice.
 */
public record TransactionChargeRequest(
        @NotNull(message = "clientRef is required")
        UUID clientRef,

        @NotBlank(message = "transactionRef is required")
        @Size(max = 100)
        String transactionRef,

        @NotNull(message = "transactionAmount is required")
        @Positive(message = "transactionAmount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        BigDecimal transactionAmount,

        @Size(max = 500)
        String description
) {
}
