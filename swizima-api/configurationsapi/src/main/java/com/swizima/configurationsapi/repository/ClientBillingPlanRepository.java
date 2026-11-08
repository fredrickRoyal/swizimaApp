package com.swizima.configurationsapi.repository;

import com.swizima.configurationsapi.entity.ClientBillingPlan;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import com.swizima.configurationsapi.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientBillingPlanRepository extends JpaRepository<ClientBillingPlan, UUID> {

    Optional<ClientBillingPlan> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    Optional<ClientBillingPlan> findByClientRefAndRecordStatusNot(UUID clientRef, RecordStatus recordStatus);

    boolean existsByClientRef(UUID clientRef);

    @Query("""
            SELECT p FROM ClientBillingPlan p
            WHERE p.recordStatus <> :excluded
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<ClientBillingPlan> search(@Param("status") BillingPlanStatus status,
                                   @Param("excluded") RecordStatus excluded,
                                   Pageable pageable);
}
