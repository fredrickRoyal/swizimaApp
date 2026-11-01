package com.swizima.usersapi.repository;

import com.swizima.usersapi.entity.ClientUser;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.RecordStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientUserRepository extends JpaRepository<ClientUser, UUID> {

    Optional<ClientUser> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    /** Existing link of a person (phone or email) to a client. */
    @Query("""
            SELECT u FROM ClientUser u
            WHERE u.recordStatus <> :excluded
              AND u.clientRef = :clientRef
              AND ((:phone IS NOT NULL AND u.userPhoneNumber = :phone)
                OR (:email IS NOT NULL AND u.userEmail = :email))
            """)
    List<ClientUser> findLinks(@Param("clientRef") UUID clientRef,
                               @Param("phone") String phone,
                               @Param("email") String email,
                               @Param("excluded") RecordStatus excluded);

    /** Is this phone/email already linked to the client (ignoring {@code selfId}, may be null)? */
    @Query("""
            SELECT COUNT(u) > 0 FROM ClientUser u
            WHERE u.recordStatus <> :excluded
              AND u.clientRef = :clientRef
              AND (CAST(:selfId AS uuid) IS NULL OR u.id <> :selfId)
              AND ((:phone IS NOT NULL AND u.userPhoneNumber = :phone)
                OR (:email IS NOT NULL AND u.userEmail = :email))
            """)
    boolean existsLink(@Param("clientRef") UUID clientRef,
                       @Param("phone") String phone,
                       @Param("email") String email,
                       @Param("selfId") UUID selfId,
                       @Param("excluded") RecordStatus excluded);

    /** All client links for a person, matched on phone or email. */
    @Query("""
            SELECT u FROM ClientUser u
            WHERE u.recordStatus <> :excluded
              AND ((:phone IS NOT NULL AND u.userPhoneNumber = :phone)
                OR (:email IS NOT NULL AND u.userEmail = :email))
            ORDER BY u.defaultClient DESC, u.createdAt ASC
            """)
    List<ClientUser> findByIdentity(@Param("phone") String phone,
                                    @Param("email") String email,
                                    @Param("excluded") RecordStatus excluded);

    /** Clears the default flag on the person's other client links. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ClientUser u SET u.defaultClient = false
            WHERE u.id <> :keepId
              AND u.defaultClient = true
              AND ((:phone IS NOT NULL AND u.userPhoneNumber = :phone)
                OR (:email IS NOT NULL AND u.userEmail = :email))
            """)
    int clearOtherDefaults(@Param("keepId") UUID keepId,
                           @Param("phone") String phone,
                           @Param("email") String email);

    @Query("""
            SELECT u FROM ClientUser u
            WHERE u.recordStatus <> :excluded
              AND (:clientRef        IS NULL OR u.clientRef = :clientRef)
              AND (:staffRef         IS NULL OR u.staffRef = :staffRef)
              AND (:activationStatus IS NULL OR u.activationStatus = :activationStatus)
              AND (:defaultClient    IS NULL OR u.defaultClient = :defaultClient)
              AND (:search IS NULL
                   OR LOWER(u.userPhoneNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(u.userEmail)       LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<ClientUser> search(@Param("clientRef") UUID clientRef,
                            @Param("staffRef") UUID staffRef,
                            @Param("activationStatus") ActivationStatus activationStatus,
                            @Param("defaultClient") Boolean defaultClient,
                            @Param("search") String search,
                            @Param("excluded") RecordStatus excluded,
                            Pageable pageable);
}
