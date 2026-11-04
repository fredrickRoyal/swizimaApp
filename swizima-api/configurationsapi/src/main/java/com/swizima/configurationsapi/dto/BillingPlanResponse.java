package com.swizima.configurationsapi.dto;

import com.swizima.configurationsapi.entity.ClientBillingPlan;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Immutable, so it is safe to keep in the cache. */
public record BillingPlanResponse(
        UUID id,
        UUID clientRef,
        BigDecimal percentageRate,
        String currency,
        BigDecimal lowBalanceThreshold,
        BillingPlanStatus status,
        String notes,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public static BillingPlanResponse from(ClientBillingPlan p) {
        return new BillingPlanResponse(p.getId(), p.getClientRef(), p.getPercentageRate(), p.getCurrency(),
                p.getLowBalanceThreshold(), p.getStatus(), p.getNotes(),
                p.getCreatedAt(), p.getCreatedBy(), p.getUpdatedAt(), p.getUpdatedBy());
    }
}
