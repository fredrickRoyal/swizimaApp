package com.swizima.usersapi.repository;

import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {

    Optional<SystemUser> findByIdAndRecordStatusNot(UUID id, RecordStatus recordStatus);

    Optional<SystemUser> findByUsernameAndRecordStatusNot(String username, RecordStatus recordStatus);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByRoleAndRecordStatusNot(UserRole role, RecordStatus recordStatus);

    @Query("""
            SELECT u FROM SystemUser u
            WHERE u.recordStatus <> :excluded
              AND (:activationStatus IS NULL OR u.activationStatus = :activationStatus)
              AND (:enabled IS NULL OR u.enabled = :enabled)
              AND (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<SystemUser> search(@Param("search") String search,
                            @Param("activationStatus") ActivationStatus activationStatus,
                            @Param("enabled") Boolean enabled,
                            @Param("excluded") RecordStatus excluded,
                            Pageable pageable);
}
