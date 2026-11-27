package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.OfficerDesignation;
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
public interface OfficerDesignationRepository extends JpaRepository<OfficerDesignation, UUID> {

    Optional<OfficerDesignation> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<OfficerDesignation> findByCodeIgnoreCaseAndRecordStatusNot(String code, RecordStatus excluded);

    Optional<OfficerDesignation> findByNameIgnoreCaseAndRecordStatusNot(String name, RecordStatus excluded);

    boolean existsByCodeIgnoreCaseAndRecordStatusNot(String code, RecordStatus excluded);

    boolean existsByNameIgnoreCaseAndRecordStatusNot(String name, RecordStatus excluded);

    List<OfficerDesignation> findAllByRecordStatusNotOrderByNameAsc(RecordStatus excluded);

    @Query("""
            SELECT d FROM OfficerDesignation d
            WHERE d.recordStatus <> :excluded
              AND (:query IS NULL OR :query = ''
                   OR LOWER(d.code) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<OfficerDesignation> search(@Param("query") String query,
                                    @Param("excluded") RecordStatus excluded,
                                    Pageable pageable);
}

