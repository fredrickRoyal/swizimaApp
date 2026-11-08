package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prepaid credit top-up, e.g.
 * { "amount": 50000, "paymentMethod": "MOBILE_MONEY", "paymentReference": "MP2411041234" }.
 */
public record CreditLoadRequest(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotNull(message = "paymentMethod is required")
        CreditPaymentMethod paymentMethod,

        @Size(max = 100)
        String paymentReference,

        @Size(max = 150)
        String payerName,

        @Size(max = 30)
        String payerPhone,

        @Size(max = 150)
        String receivedBy,

        /** When the payment was made; defaults to now. */
        @PastOrPresent(message = "loadedAt cannot be in the future")
        LocalDateTime loadedAt,

        @Size(max = 500)
        String notes
) {
}
