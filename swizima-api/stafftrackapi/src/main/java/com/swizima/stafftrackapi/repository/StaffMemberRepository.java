package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.StaffMember;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;
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
public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    Optional<StaffMember> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<StaffMember> findByEmailIgnoreCaseAndRecordStatusNot(String email, RecordStatus excluded);

    boolean existsByEmailIgnoreCaseAndRecordStatusNot(String email, RecordStatus excluded);

    List<StaffMember> findAllByRecordStatusNotOrderBySurnameAscLastNameAsc(RecordStatus excluded);

    @Query("""
            SELECT s FROM StaffMember s
            WHERE s.recordStatus <> :excluded
              AND (:department IS NULL OR :department = '' OR LOWER(s.department) = LOWER(:department))
              AND (:dutyStation IS NULL OR :dutyStation = '' OR LOWER(s.dutyStation) = LOWER(:dutyStation))
              AND (:status IS NULL OR s.status = :status)
              AND (:query IS NULL OR :query = ''
                   OR LOWER(s.surname) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.designation) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.department) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.dutyStation) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<StaffMember> search(@Param("query") String query,
                             @Param("department") String department,
                             @Param("dutyStation") String dutyStation,
                             @Param("status") StaffStatus status,
                             @Param("excluded") RecordStatus excluded,
                             Pageable pageable);
}

