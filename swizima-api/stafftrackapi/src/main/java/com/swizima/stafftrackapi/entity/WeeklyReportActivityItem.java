package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportActivityItem {

    @Column(name = "activity_date")
    private LocalDate date;

    @Column(name = "officer_name", length = 180)
    private String officerName;

    @Column(name = "officer_designation", length = 180)
    private String officerDesignation;

    @Column(name = "department", length = 200)
    private String department;

    @Column(name = "duty_station", length = 180)
    private String dutyStation;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
}

