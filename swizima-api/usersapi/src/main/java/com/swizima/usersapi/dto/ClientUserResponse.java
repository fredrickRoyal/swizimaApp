package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.ClientUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientUserResponse(
        UUID id,
        UUID clientRef,
        UUID staffRef,
        UUID systemUserId,
        String userPhoneNumber,
        String userEmail,
        ActivationStatus activationStatus,
        boolean isDefaultClient,
        RecordStatus recordStatus,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static ClientUserResponse from(ClientUser u) {
        return new ClientUserResponse(u.getId(), u.getClientRef(), u.getStaffRef(), u.getSystemUserId(), u.getUserPhoneNumber(), u.getUserEmail(),
                u.getActivationStatus(), u.isDefaultClient(), u.getRecordStatus(),
                u.getCreatedBy(), u.getCreatedAt(), u.getUpdatedBy(), u.getUpdatedAt());
    }
}
