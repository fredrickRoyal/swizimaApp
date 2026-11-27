package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "system_user_accounts", indexes = {
        @Index(name = "idx_sys_user_email", columnList = "email"),
        @Index(name = "idx_sys_user_staff_id", columnList = "staff_id")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserAccount extends ParentEntity {

    @Column(name = "staff_id")
    private UUID staffId;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "username", nullable = false, length = 180)
    private String username;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Column(name = "designation", nullable = false, length = 180)
    private String designation;

    @Column(name = "department", nullable = false, length = 200)
    private String department;

    @Column(name = "duty_station", nullable = false, length = 180)
    private String dutyStation;

    @Column(name = "role", nullable = false, length = 40)
    private String role;

    @Column(name = "preferred_otp_channel", nullable = false, length = 30)
    private String preferredOtpChannel;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "last_login_at", length = 60)
    private String lastLoginAt;
}

