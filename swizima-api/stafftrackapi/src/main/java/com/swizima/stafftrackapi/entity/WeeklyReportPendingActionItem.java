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
public class WeeklyReportPendingActionItem {

    @Column(name = "action_item", columnDefinition = "TEXT")
    private String item;

    @Column(name = "source", length = 220)
    private String source;

    @Column(name = "responsible_officer", length = 180)
    private String responsibleOfficer;

    @Column(name = "department", length = 200)
    private String department;

    @Column(name = "duty_station", length = 180)
    private String dutyStation;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "status", length = 80)
    private String status;
}

