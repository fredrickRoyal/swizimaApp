package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.SupervisorConfirmationDto;
import com.swizima.stafftrackapi.entity.SupervisorConfirmation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.SupervisorConfirmationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack/supervisor-confirmations")
@RequiredArgsConstructor
@Tag(name = "Supervisor Confirmations", description = "Track and update supervisor confirmation (NEW or SEEN) for Out-of-Office Notifications")
public class SupervisorConfirmationController {

    private final SupervisorConfirmationRepository confirmationRepository;

    public record ConfirmationUpdateRequest(
            SupervisorConfirmationStatus confirmationStatus,
            String confirmedBy
    ) {
    }

    @GetMapping("/all")
    @Operation(summary = "List all supervisor confirmations across notifications")
    public ResponseEntity<List<SupervisorConfirmationDto>> getAllConfirmations() {
        List<SupervisorConfirmationDto> list = confirmationRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .map(SupervisorConfirmationDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/notification/{notificationId}")
    @Operation(summary = "List supervisor confirmations for a specific Out-of-Office Notification")
    public ResponseEntity<List<SupervisorConfirmationDto>> getByNotification(@PathVariable UUID notificationId) {
        List<SupervisorConfirmationDto> list = confirmationRepository
                .findAllByNotificationIdAndRecordStatusNotOrderByCreatedAtAsc(notificationId, RecordStatus.DELETED)
                .stream()
                .map(SupervisorConfirmationDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    @Operation(summary = "Mark a specific supervisor confirmation record as SEEN or NEW")
    public ResponseEntity<SupervisorConfirmationDto> updateConfirmationStatus(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmationUpdateRequest request) {
        SupervisorConfirmation entity = confirmationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("SupervisorConfirmation with id '" + id + "' not found"));

        SupervisorConfirmationStatus next = request != null && request.confirmationStatus() != null
                ? request.confirmationStatus()
                : SupervisorConfirmationStatus.SEEN;

        LocalDateTime now = next == SupervisorConfirmationStatus.SEEN ? LocalDateTime.now() : null;
        String actor = request != null && request.confirmedBy() != null && !request.confirmedBy().isBlank()
                ? request.confirmedBy().trim()
                : (entity.getSupervisorName() != null ? entity.getSupervisorName() : "Supervisor");

        entity.setConfirmationStatus(next);
        entity.setSeenAt(now);
        entity.setConfirmedBy(next == SupervisorConfirmationStatus.SEEN ? actor : null);

        if (entity.getNotification() != null) {
            entity.getNotification().setSupervisorConfirmationStatus(next);
            entity.getNotification().setSupervisorSeenAt(now);
            entity.getNotification().setSupervisorConfirmedBy(next == SupervisorConfirmationStatus.SEEN ? actor : null);
        }

        return ResponseEntity.ok(SupervisorConfirmationDto.from(confirmationRepository.save(entity)));
    }
}

