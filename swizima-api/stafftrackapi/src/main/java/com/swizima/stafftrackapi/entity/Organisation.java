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
@Table(name = "organisations", indexes = {
        @Index(name = "idx_org_code", columnList = "code"),
        @Index(name = "idx_org_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Organisation extends ParentEntity {

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 220)
    private String name;

    @Column(name = "organisation_type", nullable = false, length = 120)
    private String organisationType;

    @Column(name = "contact_person", length = 180)
    private String contactPerson;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "address", length = 250)
    private String address;

    @Column(name = "status", nullable = false, length = 40)
    private String status;
}

