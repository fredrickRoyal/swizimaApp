package com.swizima.stafftrackapi.entity;

import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Out-of-Office Notification (Section A) recording an officer's travel, meeting,
 * workshop, or field assignment, planned deliverables, and co-attending OPM officers.
 */
@Entity
@Table(name = "out_of_office_notifications", indexes = {
        @Index(name = "idx_ooo_reference_number", columnList = "reference_number"),
        @Index(name = "idx_ooo_department", columnList = "department"),
        @Index(name = "idx_ooo_duty_station", columnList = "duty_station"),
        @Index(name = "idx_ooo_status", columnList = "status"),
        @Index(name = "idx_ooo_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OutOfOfficeNotification extends ParentEntity {

    @Column(name = "reference_number", nullable = false, length = 40)
    private String referenceNumber;

    @Column(name = "staff_member_id")
    private UUID staffMemberId;

    @Column(name = "officer_name", nullable = false, length = 180)
    private String officerName;

    @Column(name = "officer_role", nullable = false, length = 180)
    private String officerRole;

    @Column(name = "department", nullable = false, length = 200)
    private String department;

    @Column(name = "duty_station", nullable = false, length = 180)
    private String dutyStation;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "engagement_type", nullable = false, length = 40)
    private EngagementType engagementType = EngagementType.FIELD_ASSIGNMENT;

    @Column(name = "destination", nullable = false, length = 250)
    private String destination;

    @Column(name = "host_organization", nullable = false, length = 250)
    private String hostOrganization;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "out_of_office_purposes",
            joinColumns = @JoinColumn(name = "notification_id")
    )
    @OrderColumn(name = "item_order")
    @Column(name = "purpose_item", columnDefinition = "TEXT")
    private List<String> purposeList = new ArrayList<>();

    @Builder.Default
    @Column(name = "attending_with_others", nullable = false)
    private boolean attendingWithOthers = false;

    @Builder.Default
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OutOfOfficeAttendingOfficer> attendingOfficers = new ArrayList<>();

    @Column(name = "acting_officer", length = 180)
    private String actingOfficer;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutOfOfficeStatus status = OutOfOfficeStatus.ACTIVE_AWAY;

    @Column(name = "management_action_required", columnDefinition = "TEXT")
    private String managementActionRequired;

    @Column(name = "unit_head_supervisor_id")
    private UUID unitHeadSupervisorId;

    @Column(name = "unit_head_name", length = 180)
    private String unitHeadName;

    @Column(name = "unit_head_email", length = 180)
    private String unitHeadEmail;

    @Column(name = "unit_head_phone", length = 40)
    private String unitHeadPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_head_approval_status", length = 30)
    private UnitHeadApprovalStatus unitHeadApprovalStatus;

    @Column(name = "unit_head_reviewed_at")
    private LocalDateTime unitHeadReviewedAt;

    @Column(name = "unit_head_reviewed_by", length = 180)
    private String unitHeadReviewedBy;

    @Column(name = "unit_head_comment", columnDefinition = "TEXT")
    private String unitHeadComment;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "supervisor_confirmation_status", length = 20)
    private SupervisorConfirmationStatus supervisorConfirmationStatus = SupervisorConfirmationStatus.NEW;

    @Column(name = "supervisor_seen_at")
    private LocalDateTime supervisorSeenAt;

    @Column(name = "supervisor_confirmed_by", length = 180)
    private String supervisorConfirmedBy;

    @Builder.Default
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SupervisorConfirmation> supervisorConfirmations = new ArrayList<>();

    @OneToOne(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private OutOfOfficeReportBack reportBack;
}

