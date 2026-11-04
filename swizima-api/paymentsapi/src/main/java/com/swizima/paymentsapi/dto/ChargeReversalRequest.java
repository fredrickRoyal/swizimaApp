package com.swizima.paymentsapi.dto;

import jakarta.validation.constraints.Size;

public record ChargeReversalRequest(
        @Size(max = 500)
        String reason
) {
}
