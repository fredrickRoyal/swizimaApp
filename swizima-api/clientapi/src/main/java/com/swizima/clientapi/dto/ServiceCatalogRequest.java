package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload for creating or updating a service catalog item.
 */
public record ServiceCatalogRequest(
        @NotNull(message = "Client ID is required")
        UUID clientId,

        @NotBlank(message = "Service code is required")
        @Size(max = 50)
        String serviceCode,

        @NotBlank(message = "Service name is required")
        @Size(max = 255)
        String serviceName,

        @NotNull(message = "Category is required")
        ServiceCategory category,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 13, fraction = 2)
        BigDecimal price,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @Positive(message = "Duration must be positive")
        Integer durationMinutes,

        ServiceCatalogStatus status
) {
}
