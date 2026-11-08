package com.swizima.paymentsapi.dto;

import com.swizima.paymentsapi.entity.CreditTopUpRequest;
import com.swizima.paymentsapi.entity.enums.CreditPaymentMethod;
import com.swizima.paymentsapi.entity.enums.CreditRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreditTopUpRequestResponse(
        UUID id,
        UUID clientRef,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String currency,
        CreditPaymentMethod paymentMethod,
        String paymentReference,
        String payerName,
        String payerPhone,
        String notes,
        CreditRequestStatus status,
        LocalDateTime reviewedAt,
        String reviewedBy,
        String reviewNotes,
        UUID creditLoadId,
        LocalDateTime createdAt,
        String createdBy
) {

    public static CreditTopUpRequestResponse from(CreditTopUpRequest r) {
        return new CreditTopUpRequestResponse(
                r.getId(),
                r.getClientRef(),
                r.getRequestedAmount(),
                r.getApprovedAmount(),
                r.getCurrency(),
                r.getPaymentMethod(),
                r.getPaymentReference(),
                r.getPayerName(),
                r.getPayerPhone(),
                r.getNotes(),
                r.getStatus(),
                r.getReviewedAt(),
                r.getReviewedBy(),
                r.getReviewNotes(),
                r.getCreditLoadId(),
                r.getCreatedAt(),
                r.getCreatedBy()
        );
    }
}

