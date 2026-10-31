package com.swizima.clienttransactionsapi.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Body for the clear / exit actions (all optional).
 * {@code at} defaults to now; {@code by} is who cleared/released the vehicle.
 */
public record VehicleActionRequest(
        LocalDateTime at,
        @Size(max = 150) String by,
        String comment
) {
}
