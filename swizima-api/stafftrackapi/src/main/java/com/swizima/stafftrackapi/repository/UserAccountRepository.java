package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.UserAccount;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByIdAndRecordStatusNot(UUID id, RecordStatus excluded);

    Optional<UserAccount> findByEmailIgnoreCaseAndRecordStatusNot(String email, RecordStatus excluded);

    List<UserAccount> findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus excluded);
}

