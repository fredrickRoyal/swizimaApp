package com.swizima.stafftrackapi.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_consolidated_reports", indexes = {
        @Index(name = "idx_wkr_reference_number", columnList = "reference_number"),
        @Index(name = "idx_wkr_dates", columnList = "week_start, week_end")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WeeklyConsolidatedReport extends ParentEntity {

    @Column(name = "reference_number", nullable = false, length = 40)
    private String referenceNumber;

    @Column(name = "week_label", nullable = false, length = 180)
    private String weekLabel;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "department_filter", nullable = false, length = 200)
    private String departmentFilter;

    @Column(name = "duty_station", length = 180)
    private String dutyStation;

    @Column(name = "generated_by", nullable = false, length = 180)
    private String generatedBy;

    @Column(name = "generated_by_designation", length = 180)
    private String generatedByDesignation;

    @Column(name = "generated_at", nullable = false)
    private LocalDate generatedAt;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "weekly_report_activities",
            joinColumns = @JoinColumn(name = "weekly_report_id")
    )
    @OrderColumn(name = "item_order")
    private List<WeeklyReportActivityItem> activitiesUndertaken = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "weekly_report_pending_actions",
            joinColumns = @JoinColumn(name = "weekly_report_id")
    )
    @OrderColumn(name = "item_order")
    private List<WeeklyReportPendingActionItem> pendingActions = new ArrayList<>();
}

