package com.swizima.stafftrackapi.repository;

import com.swizima.stafftrackapi.entity.NotificationChannelConfig;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationChannelConfigRepository extends JpaRepository<NotificationChannelConfig, UUID> {

    List<NotificationChannelConfig> findAllByRecordStatusNotOrderByDisplayOrderAsc(RecordStatus excludedStatus);

    Optional<NotificationChannelConfig> findByChannelCodeIgnoreCaseAndRecordStatusNot(
            String channelCode,
            RecordStatus excludedStatus
    );
}

