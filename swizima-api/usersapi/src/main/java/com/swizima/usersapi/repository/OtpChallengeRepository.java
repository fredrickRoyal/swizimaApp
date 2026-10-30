package com.swizima.usersapi.repository;

import com.swizima.usersapi.entity.OtpChallenge;
import com.swizima.usersapi.entity.enums.OtpPurpose;
import com.swizima.usersapi.entity.enums.OtpStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<OtpChallenge> findByIdAndPurpose(UUID id, OtpPurpose purpose);

    /** Invalidates any outstanding challenges so only the newest code is valid. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OtpChallenge o SET o.status = :newStatus
            WHERE o.user.id = :userId AND o.purpose = :purpose AND o.status = :currentStatus
            """)
    int updateStatusForUser(@Param("userId") UUID userId,
                            @Param("purpose") OtpPurpose purpose,
                            @Param("currentStatus") OtpStatus currentStatus,
                            @Param("newStatus") OtpStatus newStatus);
}
