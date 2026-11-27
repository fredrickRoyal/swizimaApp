package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.StaffMember;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffMemberResponse(
        UUID id,
        UUID clientRef,
        String staffCode,
        String surname,
        String lastName,
        String fullName,
        String designation,
        UUID designationId,
        String email,
        String alternativeEmail,
        String phone,
        String alternativePhone,
        String department,
        UUID departmentId,
        String dutyStation,
        UUID dutyStationId,
        StaffStatus status,
        RecordStatus recordStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static StaffMemberResponse from(StaffMember s) {
        return new StaffMemberResponse(
                s.getId(),
                s.getClientRef(),
                s.getStaffCode(),
                s.getSurname(),
                s.getLastName(),
                (s.getSurname() + " " + s.getLastName()).trim(),
                s.getDesignation(),
                s.getDesignationId(),
                s.getEmail(),
                s.getAlternativeEmail(),
                s.getPhone(),
                s.getAlternativePhone(),
                s.getDepartment(),
                s.getDepartmentId(),
                s.getDutyStation(),
                s.getDutyStationId(),
                s.getStatus(),
                s.getRecordStatus(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}

