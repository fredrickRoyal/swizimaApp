package com.swizima.stafftrackapi.entity;

import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks supervisor confirmation (NEW or SEEN) for any new Out-of-Office Notification.
 */
@Entity
@Table(name = "supervisor_confirmations", indexes = {
        @Index(name = "idx_sup_conf_notification", columnList = "notification_id"),
        @Index(name = "idx_sup_conf_supervisor", columnList = "supervisor_id"),
        @Index(name = "idx_sup_conf_status", columnList = "confirmation_status")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SupervisorConfirmation extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private OutOfOfficeNotification notification;

    @Column(name = "supervisor_id")
    private UUID supervisorId;

    @Column(name = "supervisor_name", length = 180)
    private String supervisorName;

    @Column(name = "supervisor_email", length = 180)
    private String supervisorEmail;

    @Column(name = "supervisor_phone", length = 40)
    private String supervisorPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false, length = 20)
    private SupervisorConfirmationStatus confirmationStatus = SupervisorConfirmationStatus.NEW;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    @Column(name = "confirmed_by", length = 180)
    private String confirmedBy;
}

