package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.WeeklyConsolidateRequest;
import com.swizima.stafftrackapi.dto.WeeklyConsolidatedReportResponse;
import com.swizima.stafftrackapi.service.WeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack/weekly-reports")
@RequiredArgsConstructor
@Tag(name = "Weekly Consolidated Reports", description = "Auto-consolidate and manage Weekly Activity Reports from Out-of-Office and Report Back records")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PostMapping("/consolidate")
    @Operation(summary = "Consolidate and save a new Weekly Activity Report for a date window")
    public ResponseEntity<WeeklyConsolidatedReportResponse> consolidateReport(
            @Valid @RequestBody WeeklyConsolidateRequest request) {
        WeeklyConsolidatedReportResponse created = weeklyReportService.consolidateWeeklyReport(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .replacePath("/api/v1/stafftrack/weekly-reports/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/all")
    @Operation(summary = "List all active Weekly Consolidated Reports")
    public ResponseEntity<List<WeeklyConsolidatedReportResponse>> getAllReports() {
        return ResponseEntity.ok(weeklyReportService.getAllWeeklyReports());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Weekly Consolidated Report by id")
    public ResponseEntity<WeeklyConsolidatedReportResponse> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(weeklyReportService.getWeeklyReport(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a Weekly Consolidated Report")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        weeklyReportService.deleteWeeklyReport(id);
        return ResponseEntity.noContent().build();
    }
}

