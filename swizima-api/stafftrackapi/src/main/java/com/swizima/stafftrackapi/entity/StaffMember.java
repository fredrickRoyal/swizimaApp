package com.swizima.stafftrackapi.entity;

import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Registered staff officer in the OPM Staff Directory.
 */
@Entity
@Table(name = "staff_members", indexes = {
        @Index(name = "idx_staff_members_email", columnList = "email"),
        @Index(name = "idx_staff_members_department", columnList = "department"),
        @Index(name = "idx_staff_members_duty_station", columnList = "duty_station")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StaffMember extends ParentEntity {

    @Column(name = "staff_code", length = 40)
    private String staffCode;

    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "designation", nullable = false, length = 180)
    private String designation;

    @Column(name = "designation_id")
    private UUID designationId;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "alternative_email", length = 180)
    private String alternativeEmail;

    @Column(name = "phone", nullable = false, length = 40)
    private String phone;

    @Column(name = "alternative_phone", length = 40)
    private String alternativePhone;

    @Column(name = "department", nullable = false, length = 200)
    private String department;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "duty_station", nullable = false, length = 180)
    private String dutyStation;

    @Column(name = "duty_station_id")
    private UUID dutyStationId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StaffStatus status = StaffStatus.ACTIVE;
}

