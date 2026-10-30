package com.swizima.usersapi.entity;

import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.OtpPurpose;
import com.swizima.usersapi.entity.enums.OtpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * A one-time password issued to a user. The {@code id} doubles as the opaque
 * reference the client sends back when verifying. Only a hash of the code is stored.
 */
@Entity
@Table(name = "otp_challenges",
        indexes = @Index(name = "idx_otp_user_purpose_status", columnList = "user_id, purpose, status"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"codeHash", "user"})
public class OtpChallenge extends ParentEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private OtpChannel channel;

    /** Email address or phone number the code was sent to. */
    @Column(name = "destination", nullable = false, length = 150)
    private String destination;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OtpStatus status = OtpStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
