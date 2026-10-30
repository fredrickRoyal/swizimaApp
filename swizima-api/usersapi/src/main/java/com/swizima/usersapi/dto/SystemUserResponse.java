package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import com.swizima.usersapi.entity.enums.UsernameType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User representation returned by the API. Never includes the password hash.
 */
public record SystemUserResponse(
        UUID id,
        String username,
        UsernameType usernameType,
        String email,
        String phoneNumber,
        OtpChannel preferredOtpChannel,
        boolean enabled,
        ActivationStatus activationStatus,
        UserRole role,
        boolean passwordSystemGenerated,
        boolean mustChangePassword,
        LocalDateTime passwordChangedAt,
        LocalDateTime lastLoginAt,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        RecordStatus recordStatus
) {

    public static SystemUserResponse from(SystemUser u) {
        return new SystemUserResponse(
                u.getId(),
                u.getUsername(),
                u.getUsernameType(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getPreferredOtpChannel(),
                u.isEnabled(),
                u.getActivationStatus(),
                u.getRole(),
                u.isPasswordSystemGenerated(),
                u.isPasswordSystemGenerated(),
                u.getPasswordChangedAt(),
                u.getLastLoginAt(),
                u.getCreatedBy(),
                u.getCreatedAt(),
                u.getUpdatedBy(),
                u.getUpdatedAt(),
                u.getRecordStatus()
        );
    }
}
