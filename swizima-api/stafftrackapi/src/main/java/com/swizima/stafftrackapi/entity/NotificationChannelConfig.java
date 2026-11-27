package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_channel_configs", indexes = {
        @Index(name = "idx_ncc_channel_code", columnList = "channel_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class NotificationChannelConfig extends ParentEntity {

    @Column(name = "channel_code", nullable = false, unique = true, length = 40)
    private String channelCode;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "description", length = 320)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "otp_enabled", nullable = false)
    private boolean otpEnabled;

    @Column(name = "supervisor_alert_enabled", nullable = false)
    private boolean supervisorAlertEnabled;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}

