package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for recording or updating an Out-of-Office Notification (Section A).
 */
public record OutOfOfficeNotificationRequest(
        UUID clientRef,

        UUID staffMemberId,

        @NotBlank(message = "officerName is required")
        @Size(max = 180)
        String officerName,

        @NotBlank(message = "officerRole is required")
        @Size(max = 180)
        String officerRole,

        @NotBlank(message = "department is required")
        @Size(max = 200)
        String department,

        @NotBlank(message = "dutyStation is required")
        @Size(max = 180)
        String dutyStation,

        @NotNull(message = "engagementType is required")
        EngagementType engagementType,

        @NotBlank(message = "destination is required")
        @Size(max = 250)
        String destination,

        @NotBlank(message = "hostOrganization is required")
        @Size(max = 250)
        String hostOrganization,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        String purpose,

        List<String> purposeList,

        Boolean attendingWithOthers,

        List<@Valid AttendingOfficerDto> attendingOfficers,

        @Size(max = 180)
        String actingOfficer,

        @Size(max = 40)
        String contactPhone,

        OutOfOfficeStatus status,

        String managementActionRequired
) {
}

