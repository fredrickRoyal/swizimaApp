package com.swizima.clienttransactionsapi.dto;

import com.swizima.clienttransactionsapi.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** POST /vehicle-services/{id}/payments body. {@code paymentDateTime} defaults to now. */
public record TransactionPaymentRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 100) String referenceNumber,
        @Size(max = 150) String paidByName,
        @Size(max = 30) String paidByPhone,
        @Size(max = 150) String receivedBy,
        LocalDateTime paymentDateTime,
        String comment
) {
}
