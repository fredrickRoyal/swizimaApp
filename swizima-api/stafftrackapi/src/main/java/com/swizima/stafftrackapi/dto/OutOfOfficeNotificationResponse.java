package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OutOfOfficeNotificationResponse(
        UUID id,
        UUID clientRef,
        String referenceNumber,
        UUID staffMemberId,
        String officerName,
        String officerRole,
        String department,
        String dutyStation,
        EngagementType engagementType,
        String destination,
        String hostOrganization,
        LocalDate startDate,
        LocalDate endDate,
        String purpose,
        List<String> purposeList,
        boolean attendingWithOthers,
        List<AttendingOfficerDto> attendingOfficers,
        String actingOfficer,
        String contactPhone,
        OutOfOfficeStatus status,
        String managementActionRequired,
        UUID unitHeadSupervisorId,
        String unitHeadName,
        String unitHeadEmail,
        String unitHeadPhone,
        UnitHeadApprovalStatus unitHeadApprovalStatus,
        LocalDateTime unitHeadReviewedAt,
        String unitHeadReviewedBy,
        String unitHeadComment,
        SupervisorConfirmationStatus supervisorConfirmationStatus,
        LocalDateTime supervisorSeenAt,
        String supervisorConfirmedBy,
        List<SupervisorConfirmationDto> supervisorConfirmations,
        OutOfOfficeReportBackResponse reportBack,
        RecordStatus recordStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutOfOfficeNotificationResponse from(OutOfOfficeNotification n) {
        List<SupervisorConfirmationDto> confirmations = n.getSupervisorConfirmations() != null
                ? n.getSupervisorConfirmations().stream().map(SupervisorConfirmationDto::from).toList()
                : List.of();
        SupervisorConfirmationStatus computedConfStatus = n.getSupervisorConfirmationStatus() != null
                ? n.getSupervisorConfirmationStatus()
                : (confirmations.stream().anyMatch(c -> c.confirmationStatus() == SupervisorConfirmationStatus.SEEN)
                ? SupervisorConfirmationStatus.SEEN
                : SupervisorConfirmationStatus.NEW);

        return new OutOfOfficeNotificationResponse(
                n.getId(),
                n.getClientRef(),
                n.getReferenceNumber(),
                n.getStaffMemberId(),
                n.getOfficerName(),
                n.getOfficerRole(),
                n.getDepartment(),
                n.getDutyStation(),
                n.getEngagementType(),
                n.getDestination(),
                n.getHostOrganization(),
                n.getStartDate(),
                n.getEndDate(),
                n.getPurpose(),
                n.getPurposeList() != null ? List.copyOf(n.getPurposeList()) : List.of(),
                n.isAttendingWithOthers(),
                n.getAttendingOfficers() != null
                        ? n.getAttendingOfficers().stream().map(AttendingOfficerDto::from).toList()
                        : List.of(),
                n.getActingOfficer(),
                n.getContactPhone(),
                n.getStatus(),
                n.getManagementActionRequired(),
                n.getUnitHeadSupervisorId(),
                n.getUnitHeadName(),
                n.getUnitHeadEmail(),
                n.getUnitHeadPhone(),
                n.getUnitHeadApprovalStatus() != null
                        ? n.getUnitHeadApprovalStatus()
                        : (n.getUnitHeadName() != null && !n.getUnitHeadName().isBlank()
                        ? UnitHeadApprovalStatus.PENDING_UNIT_HEAD
                        : UnitHeadApprovalStatus.NOT_REQUIRED),
                n.getUnitHeadReviewedAt(),
                n.getUnitHeadReviewedBy(),
                n.getUnitHeadComment(),
                computedConfStatus,
                n.getSupervisorSeenAt(),
                n.getSupervisorConfirmedBy(),
                confirmations,
                OutOfOfficeReportBackResponse.from(n.getReportBack()),
                n.getRecordStatus(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}

