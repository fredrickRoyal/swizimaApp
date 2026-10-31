package com.swizima.clienttransactionsapi.dto;

import com.swizima.clienttransactionsapi.entity.TransactionPayment;
import com.swizima.clienttransactionsapi.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionPaymentResponse(
        UUID id,
        UUID transactionId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String referenceNumber,
        String paidByName,
        String paidByPhone,
        String receivedBy,
        LocalDateTime paymentDateTime,
        String comment,
        LocalDateTime createdAt
) {
    public static TransactionPaymentResponse from(TransactionPayment p) {
        return new TransactionPaymentResponse(p.getId(), p.getTransaction().getId(), p.getAmount(), p.getPaymentMethod(),
                p.getReferenceNumber(), p.getPaidByName(), p.getPaidByPhone(), p.getReceivedBy(),
                p.getPaymentDateTime(), p.getComment(), p.getCreatedAt());
    }
}
