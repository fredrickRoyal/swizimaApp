package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.DutyStation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DutyStationRepository extends JpaRepository<DutyStation, UUID> {

    Optional<DutyStation> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<DutyStation> findByCodeIgnoreCaseAndRecordStatusNot(String code, RecordStatus excluded);

    Optional<DutyStation> findByNameIgnoreCaseAndRecordStatusNot(String name, RecordStatus excluded);

    boolean existsByCodeIgnoreCaseAndRecordStatusNot(String code, RecordStatus excluded);

    boolean existsByNameIgnoreCaseAndRecordStatusNot(String name, RecordStatus excluded);

    List<DutyStation> findAllByRecordStatusNotOrderByNameAsc(RecordStatus excluded);

    @Query("""
            SELECT s FROM DutyStation s
            WHERE s.recordStatus <> :excluded
              AND (:query IS NULL OR :query = ''
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<DutyStation> search(@Param("query") String query,
                             @Param("excluded") RecordStatus excluded,
                             Pageable pageable);
}

