package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.SupervisorConfirmation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupervisorConfirmationRepository extends JpaRepository<SupervisorConfirmation, UUID> {

    List<SupervisorConfirmation> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus recordStatus);

    List<SupervisorConfirmation> findAllByNotificationIdAndRecordStatusNotOrderByCreatedAtAsc(
            UUID notificationId, RecordStatus recordStatus);

    Optional<SupervisorConfirmation> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);
}

