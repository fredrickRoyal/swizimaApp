package com.swizima.usersapi.entity;

import com.swizima.usersapi.entity.enums.AccountCategory;
import com.swizima.usersapi.entity.enums.ActivationStatus;
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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

/**
 * Links a person (identified by phone and/or email) to a client they can access.
 * A person may belong to several clients; {@code defaultClient} marks the one opened by default.
 * {@code clientRef} is the client id in clientapi (separate database, so no FK).
 */
@Entity
@Table(name = "client_users", indexes = {
        @Index(name = "idx_client_users_client_ref", columnList = "client_ref"),
        @Index(name = "idx_client_users_phone", columnList = "user_phone_number"),
        @Index(name = "idx_client_users_email", columnList = "user_email"),
        @Index(name = "idx_client_users_staff_ref", columnList = "staff_ref")
})
@Check(name = "chk_client_users_contact", constraints = "user_phone_number IS NOT NULL OR user_email IS NOT NULL")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ClientUser extends ParentEntity {

    @Column(name = "client_ref", nullable = false)
    private UUID clientRef;

    /** Optional: the client staff record (clientapi) this account belongs to. */
    @Column(name = "staff_ref")
    private UUID staffRef;

    /** Optional: the system_users account used to sign in. */
    @Column(name = "system_user_id")
    private UUID systemUserId;

    /** E.164, e.g. +256700123456. Required when no email is given. */
    @Column(name = "user_phone_number", length = 20)
    private String userPhoneNumber;

    /** Lower-cased. Required when no phone number is given. */
    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'CLIENT_ADMIN'")
    @Column(name = "account_category", nullable = false, length = 20)
    private AccountCategory accountCategory = AccountCategory.CLIENT_ADMIN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "activation_status", nullable = false, length = 30)
    private ActivationStatus activationStatus = ActivationStatus.PENDING_ACTIVATION;

    @Builder.Default
    @Column(name = "is_default_client", nullable = false)
    private boolean defaultClient = false;
}
