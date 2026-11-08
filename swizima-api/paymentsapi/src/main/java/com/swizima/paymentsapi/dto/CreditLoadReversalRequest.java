package com.swizima.paymentsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditLoadReversalRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 500)
        String reason,

        @Size(max = 150)
        String reversedBy
) {
}
