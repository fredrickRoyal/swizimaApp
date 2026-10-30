package com.swizima.clientapi.repository;

import com.swizima.clientapi.entity.Client;
import com.swizima.clientapi.entity.enums.ClientStatus;
import com.swizima.clientapi.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    Optional<Client> findByClientCodeIgnoreCaseAndRecordStatusNot(String clientCode, RecordStatus recordStatus);

    boolean existsByClientCodeIgnoreCase(String clientCode);

    boolean existsByClientCodeIgnoreCaseAndIdNot(String clientCode, UUID id);

    Page<Client> findAllByRecordStatusNot(RecordStatus recordStatus, Pageable pageable);

    Page<Client> findAllByStatusAndRecordStatusNot(ClientStatus status, RecordStatus recordStatus, Pageable pageable);

    @Query("""
            SELECT c FROM Client c
            WHERE c.recordStatus <> :excluded
              AND (LOWER(c.clientName)   LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.clientCode)   LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.tradingName)  LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.email)        LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.phoneNumber)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Client> search(@Param("search") String search,
                        @Param("excluded") RecordStatus excluded,
                        Pageable pageable);
}
