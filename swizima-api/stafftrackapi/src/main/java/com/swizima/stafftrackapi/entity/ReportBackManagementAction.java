package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Management follow-up action assigned to a responsible officer during Report Back.
 */
@Entity
@Table(name = "report_back_management_actions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ReportBackManagementAction extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_back_id", nullable = false)
    private OutOfOfficeReportBack reportBack;

    @Column(name = "follow_up_action", nullable = false, columnDefinition = "TEXT")
    private String followUpAction;

    @Column(name = "responsible_staff_id")
    private UUID responsibleStaffId;

    @Column(name = "responsible_officer", nullable = false, length = 180)
    private String responsibleOfficer;

    @Column(name = "target_date")
    private LocalDate targetDate;
}

