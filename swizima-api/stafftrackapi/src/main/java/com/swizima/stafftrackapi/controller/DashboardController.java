package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.DashboardSummaryResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stafftrack/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Real-time executive dashboard metrics and live roster of officer deployments")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get Dashboard KPI metrics, engagement breakdown, and filtered deployment roster")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) OutOfOfficeStatus status) {
        return ResponseEntity.ok(dashboardService.getDashboardSummary(query, department, status));
    }

    @GetMapping("/deployments")
    @Operation(summary = "Get filtered list of Who Is Away, Location, Duration & Purpose deployments")
    public ResponseEntity<List<OutOfOfficeNotificationResponse>> getDashboardDeployments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) OutOfOfficeStatus status) {
        return ResponseEntity.ok(dashboardService.getDashboardDeployments(query, department, status));
    }
}

