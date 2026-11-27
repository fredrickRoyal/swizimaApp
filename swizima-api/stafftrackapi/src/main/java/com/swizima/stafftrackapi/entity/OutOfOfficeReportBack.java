package com.swizima.stafftrackapi.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

/**
 * Report Back on an Out-of-Office Engagement (Section B), containing actual return date,
 * stakeholders met, key issues raised/matters discussed, achievements/outcomes,
 * management follow-up actions, and supporting attachments.
 */
@Entity
@Table(name = "out_of_office_report_backs")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OutOfOfficeReportBack extends ParentEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false, unique = true)
    private OutOfOfficeNotification notification;

    @Column(name = "actual_return_date", nullable = false)
    private LocalDate actualReturnDate;

    @Column(name = "stakeholders_met", columnDefinition = "TEXT")
    private String stakeholdersMet;

    @Column(name = "what_was_done", columnDefinition = "TEXT")
    private String whatWasDone;

    @Column(name = "what_was_discussed", columnDefinition = "TEXT")
    private String whatWasDiscussed;

    @Column(name = "what_was_achieved", columnDefinition = "TEXT")
    private String whatWasAchieved;

    @Column(name = "outstanding_issues", columnDefinition = "TEXT")
    private String outstandingIssues;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "report_back_key_issues",
            joinColumns = @JoinColumn(name = "report_back_id")
    )
    @OrderColumn(name = "item_order")
    @Column(name = "issue_item", columnDefinition = "TEXT")
    private List<String> keyIssuesList = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "report_back_achievements",
            joinColumns = @JoinColumn(name = "report_back_id")
    )
    @OrderColumn(name = "item_order")
    @Column(name = "achievement_item", columnDefinition = "TEXT")
    private List<String> achievementsList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reportBack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReportBackManagementAction> managementActions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reportBack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReportBackAttachment> attachments = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false)
    private LocalDate submittedAt;
}

