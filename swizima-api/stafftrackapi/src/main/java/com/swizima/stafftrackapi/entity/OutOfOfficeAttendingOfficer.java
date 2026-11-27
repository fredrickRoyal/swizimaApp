package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Co-attending OPM officer linked to an Out-of-Office Notification (selected from
 * registered staff or typed manually).
 */
@Entity
@Table(name = "out_of_office_attending_officers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OutOfOfficeAttendingOfficer extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private OutOfOfficeNotification notification;

    @Column(name = "staff_member_id")
    private UUID staffMemberId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "designation", length = 180)
    private String designation;

    @Column(name = "department", length = 200)
    private String department;

    @Column(name = "duty_station", length = 180)
    private String dutyStation;

    @Builder.Default
    @Column(name = "is_custom", nullable = false)
    private boolean custom = false;
}

