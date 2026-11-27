package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupervisorRepository extends JpaRepository<Supervisor, UUID> {

    List<Supervisor> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus recordStatus);

    Optional<Supervisor> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);
}

