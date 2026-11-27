package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.WeeklyConsolidatedReport;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyConsolidatedReportRepository extends JpaRepository<WeeklyConsolidatedReport, UUID> {

    Optional<WeeklyConsolidatedReport> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    List<WeeklyConsolidatedReport> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus excluded);

    long countByRecordStatusNot(RecordStatus excluded);
}

