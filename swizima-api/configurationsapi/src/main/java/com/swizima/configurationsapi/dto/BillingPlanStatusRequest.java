package com.swizima.configurationsapi.dto;

import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import jakarta.validation.constraints.NotNull;

public record BillingPlanStatusRequest(
        @NotNull(message = "status is required")
        BillingPlanStatus status
) {
}
