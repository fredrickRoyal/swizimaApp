package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload submitted by Super Admin when approving a client's credit top-up request
 * and confirming the actual credit amount to load into their wallet.
 */
public record CreditTopUpApprovalRequest(
        @NotNull(message = "approvedAmount is required")
        @Positive(message = "approvedAmount must be greater than zero")
        @Digits(integer = 13, fraction = 2)
        BigDecimal approvedAmount,

        CreditPaymentMethod paymentMethod,

        @Size(max = 100)
        String paymentReference,

        @Size(max = 150)
        String reviewedBy,

        @Size(max = 500)
        String reviewNotes
) {
}

