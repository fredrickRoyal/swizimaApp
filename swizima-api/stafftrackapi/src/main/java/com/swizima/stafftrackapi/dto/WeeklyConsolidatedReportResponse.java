package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.WeeklyConsolidatedReport;
import com.swizima.stafftrackapi.entity.WeeklyReportActivityItem;
import com.swizima.stafftrackapi.entity.WeeklyReportPendingActionItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record WeeklyConsolidatedReportResponse(
        UUID id,
        UUID clientRef,
        String referenceNumber,
        String weekLabel,
        LocalDate weekStart,
        LocalDate weekEnd,
        String departmentFilter,
        String dutyStation,
        String generatedBy,
        String generatedByDesignation,
        LocalDate generatedAt,
        String executiveSummary,
        List<ActivityItemDto> activitiesUndertaken,
        List<PendingActionItemDto> pendingActions,
        LocalDateTime createdAt
) {

    public record ActivityItemDto(
            LocalDate date,
            String officerName,
            String officerDesignation,
            String department,
            String dutyStation,
            String category,
            String summary
    ) {
        public static ActivityItemDto from(WeeklyReportActivityItem item) {
            return new ActivityItemDto(
                    item.getDate(),
                    item.getOfficerName(),
                    item.getOfficerDesignation(),
                    item.getDepartment(),
                    item.getDutyStation(),
                    item.getCategory(),
                    item.getSummary()
            );
        }
    }

    public record PendingActionItemDto(
            String item,
            String source,
            String responsibleOfficer,
            String department,
            String dutyStation,
            LocalDate targetDate,
            String status
    ) {
        public static PendingActionItemDto from(WeeklyReportPendingActionItem pa) {
            return new PendingActionItemDto(
                    pa.getItem(),
                    pa.getSource(),
                    pa.getResponsibleOfficer(),
                    pa.getDepartment(),
                    pa.getDutyStation(),
                    pa.getTargetDate(),
                    pa.getStatus()
            );
        }
    }

    public static WeeklyConsolidatedReportResponse from(WeeklyConsolidatedReport entity) {
        return new WeeklyConsolidatedReportResponse(
                entity.getId(),
                entity.getClientRef(),
                entity.getReferenceNumber(),
                entity.getWeekLabel(),
                entity.getWeekStart(),
                entity.getWeekEnd(),
                entity.getDepartmentFilter(),
                entity.getDutyStation(),
                entity.getGeneratedBy(),
                entity.getGeneratedByDesignation(),
                entity.getGeneratedAt(),
                entity.getExecutiveSummary(),
                entity.getActivitiesUndertaken() != null
                        ? entity.getActivitiesUndertaken().stream().map(ActivityItemDto::from).toList()
                        : List.of(),
                entity.getPendingActions() != null
                        ? entity.getPendingActions().stream().map(PendingActionItemDto::from).toList()
                        : List.of(),
                entity.getCreatedAt()
        );
    }
}

