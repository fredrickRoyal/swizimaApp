package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.WeeklyConsolidateRequest;
import com.swizima.stafftrackapi.dto.WeeklyConsolidatedReportResponse;
import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import com.swizima.stafftrackapi.entity.WeeklyConsolidatedReport;
import com.swizima.stafftrackapi.entity.WeeklyReportActivityItem;
import com.swizima.stafftrackapi.entity.WeeklyReportPendingActionItem;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.BusinessRuleException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.OutOfOfficeNotificationRepository;
import com.swizima.stafftrackapi.repository.WeeklyConsolidatedReportRepository;
import com.swizima.stafftrackapi.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final WeeklyConsolidatedReportRepository weeklyReportRepository;
    private final OutOfOfficeNotificationRepository outOfOfficeRepository;

    @Override
    @Transactional
    public WeeklyConsolidatedReportResponse consolidateWeeklyReport(WeeklyConsolidateRequest request) {
        if (request.weekEnd().isBefore(request.weekStart())) {
            throw new BusinessRuleException("weekEnd cannot be earlier than weekStart");
        }

        String rawDept = request.departmentFilter() != null ? request.departmentFilter().trim() : "ALL";
        boolean allDepts = rawDept.isEmpty() || rawDept.equalsIgnoreCase("ALL") || rawDept.equalsIgnoreCase("All Departments");

        List<OutOfOfficeNotification> matchingNotifications = outOfOfficeRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(o -> o.getStatus() != OutOfOfficeStatus.CANCELLED)
                .filter(o -> allDepts || (o.getDepartment() != null && o.getDepartment().equalsIgnoreCase(rawDept)))
                .filter(o -> o.getStartDate() != null && o.getEndDate() != null
                        && !o.getStartDate().isAfter(request.weekEnd())
                        && !o.getEndDate().isBefore(request.weekStart()))
                .toList();

        List<WeeklyReportActivityItem> activities = new ArrayList<>();
        List<WeeklyReportPendingActionItem> pendingActions = new ArrayList<>();

        for (OutOfOfficeNotification o : matchingNotifications) {
            activities.add(WeeklyReportActivityItem.builder()
                    .date(o.getStartDate())
                    .officerName(o.getOfficerName())
                    .officerDesignation(o.getOfficerRole())
                    .department(o.getDepartment())
                    .dutyStation(o.getDutyStation())
                    .category(formatEngagementLabel(o.getEngagementType()))
                    .summary(o.getDestination() + " (" + o.getHostOrganization() + ") – " + o.getPurpose())
                    .build());

            if (o.getReportBack() != null && o.getReportBack().getManagementActions() != null) {
                for (ReportBackManagementAction ma : o.getReportBack().getManagementActions()) {
                    if (ma.getFollowUpAction() == null || ma.getFollowUpAction().isBlank()) {
                        continue;
                    }
                    pendingActions.add(WeeklyReportPendingActionItem.builder()
                            .item(ma.getFollowUpAction())
                            .source("Report Back (" + o.getDestination() + ")")
                            .responsibleOfficer(ma.getResponsibleOfficer() != null && !ma.getResponsibleOfficer().isBlank()
                                    ? ma.getResponsibleOfficer()
                                    : o.getOfficerName())
                            .department(o.getDepartment())
                            .dutyStation(o.getDutyStation())
                            .targetDate(ma.getTargetDate() != null ? ma.getTargetDate() : request.weekEnd())
                            .status("Management Action")
                            .build());
                }
            }
        }

        WeeklyConsolidatedReport report = WeeklyConsolidatedReport.builder()
                .clientRef(request.clientRef())
                .referenceNumber(generateReferenceNumber())
                .weekLabel(request.weekLabel().trim())
                .weekStart(request.weekStart())
                .weekEnd(request.weekEnd())
                .departmentFilter(allDepts ? "All Departments" : rawDept)
                .dutyStation(request.dutyStation() != null ? request.dutyStation().trim() : "")
                .generatedBy(request.generatedBy().trim())
                .generatedByDesignation(request.generatedByDesignation() != null ? request.generatedByDesignation().trim() : "")
                .generatedAt(LocalDate.now())
                .executiveSummary("")
                .activitiesUndertaken(activities)
                .pendingActions(pendingActions)
                .recordStatus(RecordStatus.ACTIVE)
                .build();

        return WeeklyConsolidatedReportResponse.from(weeklyReportRepository.save(report));
    }

    @Override
    public List<WeeklyConsolidatedReportResponse> getAllWeeklyReports() {
        return weeklyReportRepository.findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .map(WeeklyConsolidatedReportResponse::from)
                .toList();
    }

    @Override
    public WeeklyConsolidatedReportResponse getWeeklyReport(UUID id) {
        return WeeklyConsolidatedReportResponse.from(findReport(id));
    }

    @Override
    @Transactional
    public void deleteWeeklyReport(UUID id) {
        WeeklyConsolidatedReport report = findReport(id);
        report.setRecordStatus(RecordStatus.DELETED);
        weeklyReportRepository.save(report);
    }

    private WeeklyConsolidatedReport findReport(UUID id) {
        return weeklyReportRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Weekly Consolidated Report with id '" + id + "' not found"));
    }

    private String generateReferenceNumber() {
        long next = weeklyReportRepository.count() + 101;
        return "WKR-" + next;
    }

    private String formatEngagementLabel(EngagementType type) {
        if (type == null) return "Official Engagement";
        return switch (type) {
            case TRAVEL -> "Official Travel";
            case MEETING -> "External Meeting";
            case WORKSHOP -> "Workshop / Training";
            case FIELD_ASSIGNMENT -> "Field Assignment";
            case OFFICIAL_ENGAGEMENT -> "Official Engagement";
        };
    }
}

