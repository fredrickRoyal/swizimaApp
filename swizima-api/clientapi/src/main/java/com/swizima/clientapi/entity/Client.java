package com.swizima.clientapi.entity;

import com.swizima.clientapi.entity.enums.ClientStatus;
import com.swizima.clientapi.entity.enums.ClientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Client extends ParentEntity {

    @Column(name = "client_code", nullable = false, unique = true, length = 50)
    private String clientCode;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "trading_name")
    private String tradingName;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 30)
    private ClientType clientType;

    @Column(name = "industry_sector", length = 150)
    private String industrySector;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "alternative_phone", length = 30)
    private String alternativePhone;

    @Column(name = "address")
    private String address;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "contact_person_title", length = 100)
    private String contactPersonTitle;

    @Column(name = "contact_person_email", length = 150)
    private String contactPersonEmail;

    @Column(name = "contact_person_phone", length = 30)
    private String contactPersonPhone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClientStatus status = ClientStatus.PENDING;
}
