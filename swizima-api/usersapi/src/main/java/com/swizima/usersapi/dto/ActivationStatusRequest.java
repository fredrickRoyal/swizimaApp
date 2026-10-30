package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.ActivationStatus;
import jakarta.validation.constraints.NotNull;

public record ActivationStatusRequest(
        @NotNull(message = "Activation status is required")
        ActivationStatus activationStatus
) {
}
