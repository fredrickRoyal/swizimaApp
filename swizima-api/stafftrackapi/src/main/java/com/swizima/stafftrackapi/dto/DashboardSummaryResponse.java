package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.enums.EngagementType;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        long currentlyAwayCount,
        long currentlyInOfficeCount,
        long upcomingApprovedCount,
        long pendingApprovalCount,
        long returnedCount,
        long totalNotificationsCount,
        long registeredStaffCount,
        long departmentsCount,
        long designationsCount,
        long dutyStationsCount,
        long weeklyReportsCount,
        Map<EngagementType, Long> engagementTypeBreakdown,
        List<OutOfOfficeNotificationResponse> deployments
) {
}

