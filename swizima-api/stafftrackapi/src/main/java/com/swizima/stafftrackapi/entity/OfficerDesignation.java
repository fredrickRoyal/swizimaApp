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
 * Master Officer Designation / Job Title (e.g. Principal Officer, Settlement Commandant).
 */
@Entity
@Table(name = "officer_designations", indexes = {
        @Index(name = "idx_officer_designations_code", columnList = "code"),
        @Index(name = "idx_officer_designations_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OfficerDesignation extends ParentEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;
}

