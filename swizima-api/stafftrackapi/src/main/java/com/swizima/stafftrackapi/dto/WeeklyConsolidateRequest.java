package com.swizima.stafftrackapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record WeeklyConsolidateRequest(
        UUID clientRef,

        @NotBlank(message = "weekLabel is required")
        String weekLabel,

        @NotNull(message = "weekStart is required")
        LocalDate weekStart,

        @NotNull(message = "weekEnd is required")
        LocalDate weekEnd,

        String departmentFilter,

        String dutyStation,

        @NotBlank(message = "generatedBy is required")
        String generatedBy,

        String generatedByDesignation
) {
}

