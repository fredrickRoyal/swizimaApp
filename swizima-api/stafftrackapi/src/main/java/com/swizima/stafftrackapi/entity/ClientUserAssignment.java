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
@Table(name = "client_user_assignments", indexes = {
        @Index(name = "idx_cu_system_user_id", columnList = "system_user_id"),
        @Index(name = "idx_cu_email", columnList = "user_email")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ClientUserAssignment extends ParentEntity {

    @Column(name = "system_user_id")
    private UUID systemUserId;

    @Column(name = "user_phone_number", length = 40)
    private String userPhoneNumber;

    @Column(name = "user_email", length = 180)
    private String userEmail;

    @Column(name = "account_category", nullable = false, length = 40)
    private String accountCategory;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "client_code", nullable = false, length = 60)
    private String clientCode;

    @Column(name = "is_default_client", nullable = false)
    private boolean defaultClient;

    @Column(name = "activation_status", nullable = false, length = 40)
    private String activationStatus;
}

