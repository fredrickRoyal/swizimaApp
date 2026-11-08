package com.swizima.usersapi.repository;

import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.AccountCategory;
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

    /** Finds a user by normalized username, email (case-insensitive) or E.164 phone number. */
    @Query("""
            SELECT u FROM SystemUser u
            WHERE u.recordStatus <> :excluded
              AND (u.username = :value OR LOWER(u.email) = LOWER(:value) OR u.phoneNumber = :value)
            """)
    Optional<SystemUser> findByIdentityAndRecordStatusNot(@Param("value") String value,
                                                          @Param("excluded") RecordStatus excluded);

    /** Finds an existing user matching any of the supplied username, email or phone number. */
    @Query("""
            SELECT u FROM SystemUser u
            WHERE u.recordStatus <> :excluded
              AND (u.username = :username
                OR (:email IS NOT NULL AND LOWER(u.email) = LOWER(:email))
                OR (:phone IS NOT NULL AND u.phoneNumber = :phone))
            ORDER BY u.createdAt ASC
            """)
    java.util.List<SystemUser> findByAnyContact(@Param("username") String username,
                                                @Param("email") String email,
                                                @Param("phone") String phone,
                                                @Param("excluded") RecordStatus excluded);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByRoleAndRecordStatusNot(UserRole role, RecordStatus recordStatus);

    java.util.List<SystemUser> findByRoleAndEnabledTrueAndRecordStatusNot(UserRole role, RecordStatus recordStatus);

    /** True when another (non-deleted) user already uses this value as username, email or phone. */
    @Query("""
            SELECT COUNT(u) > 0 FROM SystemUser u
            WHERE u.recordStatus <> :excluded
              AND (:excludeId IS NULL OR u.id <> :excludeId)
              AND (u.username = :value OR LOWER(u.email) = LOWER(:value) OR u.phoneNumber = :value)
            """)
    boolean contactInUse(@Param("value") String value,
                         @Param("excludeId") UUID excludeId,
                         @Param("excluded") RecordStatus excluded);

    @Query("""
            SELECT u FROM SystemUser u
            WHERE u.recordStatus <> :excluded
              AND (:accountCategory IS NULL OR u.accountCategory = :accountCategory OR CAST(u.role AS string) = CAST(:accountCategory AS string))
              AND (:activationStatus IS NULL OR u.activationStatus = :activationStatus)
              AND (:enabled IS NULL OR u.enabled = :enabled)
              AND (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<SystemUser> search(@Param("search") String search,
                            @Param("accountCategory") AccountCategory accountCategory,
                            @Param("activationStatus") ActivationStatus activationStatus,
                            @Param("enabled") Boolean enabled,
                            @Param("excluded") RecordStatus excluded,
                            Pageable pageable);
}
