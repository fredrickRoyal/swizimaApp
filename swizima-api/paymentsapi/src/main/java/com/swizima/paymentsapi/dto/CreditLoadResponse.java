package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.CreditLoad;
import com.swizima.paymentsapi.entity.enums.CreditLoadStatus;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreditLoadResponse(
        UUID id,
        UUID walletId,
        UUID clientRef,
        BigDecimal amount,
        String currency,
        CreditPaymentMethod paymentMethod,
        String paymentReference,
        String payerName,
        String payerPhone,
        String receivedBy,
        LocalDateTime loadedAt,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String notes,
        CreditLoadStatus status,
        LocalDateTime reversedAt,
        String reversedBy,
        String reversalReason,
        LocalDateTime createdAt,
        String createdBy
) {

    public static CreditLoadResponse from(CreditLoad c) {
        return new CreditLoadResponse(
                c.getId(), c.getWallet().getId(), c.getClientRef(), c.getAmount(), c.getCurrency(),
                c.getPaymentMethod(), c.getPaymentReference(), c.getPayerName(), c.getPayerPhone(),
                c.getReceivedBy(), c.getLoadedAt(), c.getBalanceBefore(), c.getBalanceAfter(), c.getNotes(),
                c.getStatus(), c.getReversedAt(), c.getReversedBy(), c.getReversalReason(),
                c.getCreatedAt(), c.getCreatedBy());
    }
}
