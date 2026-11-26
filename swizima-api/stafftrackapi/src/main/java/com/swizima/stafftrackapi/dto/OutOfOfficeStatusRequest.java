package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import jakarta.validation.constraints.NotNull;

public record OutOfOfficeStatusRequest(
        @NotNull(message = "status is required")
        OutOfOfficeStatus status
) {
}

