package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record ReportBackManagementActionDto(
        UUID id,
        @NotBlank(message = "followUpAction is required")
        String followUpAction,
        UUID responsibleStaffId,
        @NotBlank(message = "responsibleOfficer is required")
        String responsibleOfficer,
        LocalDate targetDate
) {

    public static ReportBackManagementActionDto from(ReportBackManagementAction entity) {
        return new ReportBackManagementActionDto(
                entity.getId(),
                entity.getFollowUpAction(),
                entity.getResponsibleStaffId(),
                entity.getResponsibleOfficer(),
                entity.getTargetDate()
        );
    }
}

