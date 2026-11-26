package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.Organisation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {

    Optional<Organisation> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<Organisation> findByCodeIgnoreCaseAndRecordStatusNot(String code, RecordStatus excluded);

    Optional<Organisation> findByNameIgnoreCaseAndRecordStatusNot(String name, RecordStatus excluded);

    List<Organisation> findAllByRecordStatusNotOrderByCreatedAtAsc(RecordStatus excluded);
}

