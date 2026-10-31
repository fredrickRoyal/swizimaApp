package com.swizima.clientapi.repository;

import com.swizima.clientapi.entity.ClientStaff;
import com.swizima.clientapi.entity.enums.Gender;
import com.swizima.clientapi.entity.enums.RecordStatus;
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
public interface ClientStaffRepository extends JpaRepository<ClientStaff, UUID> {

    @EntityGraph(attributePaths = "client")
    Optional<ClientStaff> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    boolean existsByClientIdAndNinIgnoreCaseAndRecordStatusNot(UUID clientId, String nin, RecordStatus recordStatus);

    boolean existsByClientIdAndNinIgnoreCaseAndRecordStatusNotAndIdNot(UUID clientId, String nin,
                                                                        RecordStatus recordStatus, UUID id);

    /**
     * Flexible listing: any filter passed as null is ignored.
     */
    @EntityGraph(attributePaths = "client")
    @Query("""
            SELECT s FROM ClientStaff s
            WHERE s.recordStatus <> :excluded
              AND (:clientId IS NULL OR s.client.id = :clientId)
              AND (:gender   IS NULL OR s.gender    = :gender)
              AND (:search   IS NULL
                   OR LOWER(s.surname)     LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.givenName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.phoneNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.email)       LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.position)    LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(s.nin)         LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<ClientStaff> search(@Param("clientId") UUID clientId,
                             @Param("gender") Gender gender,
                             @Param("search") String search,
                             @Param("excluded") RecordStatus excluded,
                             Pageable pageable);
}
