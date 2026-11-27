package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.ClientUserAssignment;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientUserAssignmentRepository extends JpaRepository<ClientUserAssignment, UUID> {

    Optional<ClientUserAssignment> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    List<ClientUserAssignment> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus excluded);
}

