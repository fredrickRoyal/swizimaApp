package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record UnitHeadDecisionRequest(
        @NotNull(message = "decision is required (ACCEPTED or REJECTED)")
        UnitHeadApprovalStatus decision,
        String reviewedBy,
        String comment
) {
}

