package com.swizima.configurationsapi.dto;

import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Creates or updates a client's billing plan.
 * {@code clientRef} is ignored on update (a plan always belongs to one client).
 */
public record BillingPlanRequest(
        @NotNull(message = "clientRef is required")
        UUID clientRef,

        @NotNull(message = "percentageRate is required")
        @DecimalMin(value = "0.0", message = "percentageRate cannot be negative")
        @DecimalMax(value = "100.0", message = "percentageRate cannot exceed 100")
        @Digits(integer = 3, fraction = 4, message = "percentageRate allows up to 4 decimal places")
        BigDecimal percentageRate,

        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
        String currency,

        @PositiveOrZero(message = "lowBalanceThreshold cannot be negative")
        BigDecimal lowBalanceThreshold,

        BillingPlanStatus status,

        @Size(max = 500)
        String notes
) {
}
