package com.swizima.stafftrackapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Master Duty Station / Office / Refugee Settlement (e.g. OPM Main, Mbarara Refugee Desk, Nakivale RS).
 */
@Entity
@Table(name = "duty_stations", indexes = {
        @Index(name = "idx_duty_stations_code", columnList = "code"),
        @Index(name = "idx_duty_stations_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DutyStation extends ParentEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;
}

