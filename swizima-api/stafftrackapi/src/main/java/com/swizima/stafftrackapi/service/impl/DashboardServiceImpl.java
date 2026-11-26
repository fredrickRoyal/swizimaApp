package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.DashboardSummaryResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.repository.DepartmentRepository;
import com.swizima.stafftrackapi.repository.DutyStationRepository;
import com.swizima.stafftrackapi.repository.OfficerDesignationRepository;
import com.swizima.stafftrackapi.repository.OutOfOfficeNotificationRepository;
import com.swizima.stafftrackapi.repository.StaffMemberRepository;
import com.swizima.stafftrackapi.repository.WeeklyConsolidatedReportRepository;
import com.swizima.stafftrackapi.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OutOfOfficeNotificationRepository outOfOfficeRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerDesignationRepository designationRepository;
    private final DutyStationRepository dutyStationRepository;
    private final WeeklyConsolidatedReportRepository weeklyReportRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary(String query, String department, OutOfOfficeStatus status) {
        List<OutOfOfficeNotification> allActive = outOfOfficeRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED);

        long currentlyAway = allActive.stream()
                .filter(n -> n.getStatus() == OutOfOfficeStatus.ACTIVE_AWAY)
                .count();
        long upcomingApproved = allActive.stream()
                .filter(n -> n.getStatus() == OutOfOfficeStatus.APPROVED)
                .count();
        long pendingApproval = allActive.stream()
                .filter(n -> n.getStatus() == OutOfOfficeStatus.PENDING_APPROVAL)
                .count();
        long returned = allActive.stream()
                .filter(n -> n.getStatus() == OutOfOfficeStatus.RETURNED)
                .count();

        Map<EngagementType, Long> breakdown = new EnumMap<>(EngagementType.class);
        for (EngagementType type : EngagementType.values()) {
            long count = allActive.stream()
                    .filter(n -> n.getStatus() != OutOfOfficeStatus.CANCELLED && n.getEngagementType() == type)
                    .count();
            breakdown.put(type, count);
        }

        long staffCount = staffMemberRepository
                .findAllByRecordStatusNotOrderBySurnameAscLastNameAsc(RecordStatus.DELETED)
                .size();
        long currentlyInOffice = Math.max(0L, staffCount - currentlyAway);
        long deptCount = departmentRepository
                .findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .size();
        long desigCount = designationRepository
                .findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .size();
        long stationCount = dutyStationRepository
                .findAllByRecordStatusNotOrderByNameAsc(RecordStatus.DELETED)
                .size();
        long weeklyCount = weeklyReportRepository.countByRecordStatusNot(RecordStatus.DELETED);

        List<OutOfOfficeNotificationResponse> deployments = filterAndSortDeployments(allActive, query, department, status);

        return new DashboardSummaryResponse(
                currentlyAway,
                currentlyInOffice,
                upcomingApproved,
                pendingApproval,
                returned,
                allActive.size(),
                staffCount,
                deptCount,
                desigCount,
                stationCount,
                weeklyCount,
                breakdown,
                deployments
        );
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getDashboardDeployments(String query,
                                                                         String department,
                                                                         OutOfOfficeStatus status) {
        List<OutOfOfficeNotification> allActive = outOfOfficeRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED);
        return filterAndSortDeployments(allActive, query, department, status);
    }

    private List<OutOfOfficeNotificationResponse> filterAndSortDeployments(List<OutOfOfficeNotification> allActive,
                                                                           String query,
                                                                           String department,
                                                                           OutOfOfficeStatus status) {
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        String dept = department != null && !department.equalsIgnoreCase("ALL") ? department.trim() : "";

        return allActive.stream()
                .filter(n -> {
                    if (status != null) {
                        return n.getStatus() == status;
                    }
                    return n.getStatus() != OutOfOfficeStatus.CANCELLED;
                })
                .filter(n -> dept.isEmpty() || (n.getDepartment() != null && n.getDepartment().equalsIgnoreCase(dept)))
                .filter(n -> {
                    if (q.isEmpty()) return true;
                    return contains(n.getOfficerName(), q)
                            || contains(n.getOfficerRole(), q)
                            || contains(n.getDepartment(), q)
                            || contains(n.getDutyStation(), q)
                            || contains(n.getDestination(), q)
                            || contains(n.getHostOrganization(), q)
                            || contains(n.getPurpose(), q)
                            || contains(n.getReferenceNumber(), q);
                })
                .sorted(Comparator
                        .comparingInt((OutOfOfficeNotification n) -> statusPriority(n.getStatus()))
                        .thenComparing(OutOfOfficeNotification::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(OutOfOfficeNotificationResponse::from)
                .toList();
    }

    private int statusPriority(OutOfOfficeStatus status) {
        if (status == null) return 9;
        return switch (status) {
            case ACTIVE_AWAY -> 0;
            case APPROVED -> 1;
            case PENDING_APPROVAL -> 2;
            case RETURNED -> 3;
            case CANCELLED -> 4;
        };
    }

    private boolean contains(String source, String needle) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(needle);
    }
}

