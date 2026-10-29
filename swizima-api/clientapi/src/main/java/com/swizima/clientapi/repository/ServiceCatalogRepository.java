package com.swizima.clientapi.repository;

import com.swizima.clientapi.entity.ServiceCatalog;
import com.swizima.clientapi.entity.enums.RecordStatus;
import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, UUID> {

    @EntityGraph(attributePaths = "client")
    Optional<ServiceCatalog> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    boolean existsByClientIdAndServiceCodeIgnoreCase(UUID clientId, String serviceCode);

    boolean existsByClientIdAndServiceCodeIgnoreCaseAndIdNot(UUID clientId, String serviceCode, UUID id);

    /**
     * Flexible listing: any filter passed as null is ignored.
     */
    @EntityGraph(attributePaths = "client")
    @Query("""
            SELECT s FROM ServiceCatalog s
            WHERE s.recordStatus <> :excluded
              AND (:clientId IS NULL OR s.client.id = :clientId)
              AND (:category IS NULL OR s.category = :category)
              AND (:status   IS NULL OR s.status   = :status)
              AND (:search   IS NULL
                   OR LOWER(s.serviceName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.serviceCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<ServiceCatalog> search(@Param("clientId") UUID clientId,
                                @Param("category") ServiceCategory category,
                                @Param("status") ServiceCatalogStatus status,
                                @Param("search") String search,
                                @Param("excluded") RecordStatus excluded,
                                Pageable pageable);
}
