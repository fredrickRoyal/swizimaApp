package com.swizima.paymentsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload submitted by Super Admin when rejecting a client's credit top-up request.
 */
public record CreditTopUpRejectionRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 500)
        String reason,

        @Size(max = 150)
        String reviewedBy
) {
}

