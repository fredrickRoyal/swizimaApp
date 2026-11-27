package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.DashboardSummaryResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary(String query, String department, OutOfOfficeStatus status);

    List<OutOfOfficeNotificationResponse> getDashboardDeployments(String query, String department, OutOfOfficeStatus status);
}

