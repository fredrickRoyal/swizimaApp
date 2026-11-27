package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.Department;
import com.swizima.stafftrackapi.entity.DutyStation;
import com.swizima.stafftrackapi.entity.OfficerDesignation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified response record for master Departments, Officer Designations, and Duty Stations.
 */
public record MasterLookupResponse(
        UUID id,
        UUID clientRef,
        String code,
        String name,
        String description,
        RecordStatus recordStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MasterLookupResponse fromDepartment(Department d) {
        return new MasterLookupResponse(
                d.getId(),
                d.getClientRef(),
                d.getCode(),
                d.getName(),
                d.getDescription(),
                d.getRecordStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public static MasterLookupResponse fromDesignation(OfficerDesignation d) {
        return new MasterLookupResponse(
                d.getId(),
                d.getClientRef(),
                d.getCode(),
                d.getName(),
                d.getDescription(),
                d.getRecordStatus(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    public static MasterLookupResponse fromDutyStation(DutyStation s) {
        return new MasterLookupResponse(
                s.getId(),
                s.getClientRef(),
                s.getCode(),
                s.getName(),
                s.getDescription(),
                s.getRecordStatus(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}

