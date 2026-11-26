package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload submitted by a client when requesting a credit top-up.
 */
public record CreditTopUpRequestCreate(
        @NotNull(message = "requestedAmount is required")
        @Positive(message = "requestedAmount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        BigDecimal requestedAmount,

        @NotNull(message = "paymentMethod is required")
        CreditPaymentMethod paymentMethod,

        @Size(max = 100)
        String paymentReference,

        @Size(max = 150)
        String payerName,

        @Size(max = 30)
        String payerPhone,

        @Size(max = 500)
        String notes
) {
}

