package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.WeeklyConsolidateRequest;
import com.swizima.stafftrackapi.dto.WeeklyConsolidatedReportResponse;

import java.util.List;
import java.util.UUID;

public interface WeeklyReportService {

    WeeklyConsolidatedReportResponse consolidateWeeklyReport(WeeklyConsolidateRequest request);

    List<WeeklyConsolidatedReportResponse> getAllWeeklyReports();

    WeeklyConsolidatedReportResponse getWeeklyReport(UUID id);

    void deleteWeeklyReport(UUID id);
}

