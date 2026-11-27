package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
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
public interface OutOfOfficeNotificationRepository extends JpaRepository<OutOfOfficeNotification, UUID> {

    Optional<OutOfOfficeNotification> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<OutOfOfficeNotification> findByReferenceNumberIgnoreCaseAndRecordStatusNot(String referenceNumber, RecordStatus excluded);

    List<OutOfOfficeNotification> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus excluded);

    @Query("""
            SELECT n FROM OutOfOfficeNotification n
            WHERE n.recordStatus <> :excluded
              AND (:department IS NULL OR :department = '' OR LOWER(n.department) = LOWER(:department))
              AND (:dutyStation IS NULL OR :dutyStation = '' OR LOWER(n.dutyStation) = LOWER(:dutyStation))
              AND (:status IS NULL OR n.status = :status)
              AND (:engagementType IS NULL OR n.engagementType = :engagementType)
              AND (:query IS NULL OR :query = ''
                   OR LOWER(n.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.officerName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.officerRole) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.destination) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.hostOrganization) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.purpose) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(n.dutyStation) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<OutOfOfficeNotification> search(@Param("query") String query,
                                         @Param("department") String department,
                                         @Param("dutyStation") String dutyStation,
                                         @Param("status") OutOfOfficeStatus status,
                                         @Param("engagementType") EngagementType engagementType,
                                         @Param("excluded") RecordStatus excluded,
                                         Pageable pageable);
}

