package com.swizima.clientapi.entity;

import com.swizima.clientapi.entity.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A person working for a client (e.g. attendant, cashier, manager).
 */
@Entity
@Table(name = "client_staff",
        indexes = {
                @Index(name = "idx_client_staff_client", columnList = "client_id"),
                @Index(name = "idx_client_staff_client_nin", columnList = "client_id, nin")
        })
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ClientStaff extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    @Column(name = "given_name", nullable = false, length = 150)
    private String givenName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "alternative_phone", length = 30)
    private String alternativePhone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "position", length = 100)
    private String position;

    /** National Identification Number (unique per client when provided). */
    @Column(name = "nin", length = 20)
    private String nin;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
}
