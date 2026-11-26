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
@Table(name = "supervisors", indexes = {
        @Index(name = "idx_sup_email", columnList = "email"),
        @Index(name = "idx_sup_phone", columnList = "phone"),
        @Index(name = "idx_sup_entity_code", columnList = "entity_code")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Supervisor extends ParentEntity {

    @Column(name = "entity_name", length = 220)
    private String entityName;

    @Column(name = "entity_code", length = 60)
    private String entityCode;

    @Column(name = "name", length = 180)
    private String name;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "status", nullable = false, length = 40)
    private String status;
}

