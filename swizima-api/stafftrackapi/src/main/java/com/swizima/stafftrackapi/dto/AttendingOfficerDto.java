package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.OutOfOfficeAttendingOfficer;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AttendingOfficerDto(
        UUID id,
        UUID staffMemberId,
        @NotBlank(message = "attending officer name is required")
        String name,
        String designation,
        String department,
        String dutyStation,
        Boolean isCustom
) {

    public static AttendingOfficerDto from(OutOfOfficeAttendingOfficer entity) {
        return new AttendingOfficerDto(
                entity.getId(),
                entity.getStaffMemberId(),
                entity.getName(),
                entity.getDesignation(),
                entity.getDepartment(),
                entity.getDutyStation(),
                entity.isCustom()
        );
    }
}

