package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutOfOfficeReportBackRepository extends JpaRepository<OutOfOfficeReportBack, UUID> {

    Optional<OutOfOfficeReportBack> findByNotificationIdAndRecordStatusNot(UUID notificationId, RecordStatus excluded);
}

