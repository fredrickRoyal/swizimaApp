package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for changing a service catalog item's status.
 */
public record ServiceCatalogStatusRequest(
        @NotNull(message = "Status is required")
        ServiceCatalogStatus status
) {
}
