package com.swizima.stafftrackapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for creating or updating a master Department, Officer Designation, or Duty Station.
 */
public record MasterLookupRequest(
        UUID clientRef,

        @NotBlank(message = "code is required")
        @Size(max = 50, message = "code must not exceed 50 characters")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @Size(max = 500, message = "description must not exceed 500 characters")
        String description
) {
}

