package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.SupervisorConfirmation;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupervisorConfirmationDto(
        UUID id,
        UUID notificationId,
        String notificationRef,
        UUID supervisorId,
        String supervisorName,
        String supervisorEmail,
        String supervisorPhone,
        SupervisorConfirmationStatus confirmationStatus,
        LocalDateTime seenAt,
        String confirmedBy,
        LocalDateTime createdAt
) {
    public static SupervisorConfirmationDto from(SupervisorConfirmation entity) {
        return new SupervisorConfirmationDto(
                entity.getId(),
                entity.getNotification() != null ? entity.getNotification().getId() : null,
                entity.getNotification() != null ? entity.getNotification().getReferenceNumber() : null,
                entity.getSupervisorId(),
                entity.getSupervisorName(),
                entity.getSupervisorEmail(),
                entity.getSupervisorPhone(),
                entity.getConfirmationStatus() != null
                        ? entity.getConfirmationStatus()
                        : SupervisorConfirmationStatus.NEW,
                entity.getSeenAt(),
                entity.getConfirmedBy(),
                entity.getCreatedAt()
        );
    }
}

