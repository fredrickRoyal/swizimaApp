package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.enums.ClientStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for changing a client's business status.
 */
public record ClientStatusRequest(
        @NotNull(message = "Status is required")
        ClientStatus status
) {
}
