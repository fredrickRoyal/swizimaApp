package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.entity.NotificationChannelConfig;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.BusinessRuleException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.NotificationChannelConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationChannelConfigService {

    private final NotificationChannelConfigRepository repository;

    @Transactional
    public List<NotificationChannelConfig> ensureDefaultsAndList() {
        seedMissingDefaults();
        return repository.findAllByRecordStatusNotOrderByDisplayOrderAsc(RecordStatus.DELETED);
    }

    @Transactional
    public void seedMissingDefaults() {
        ensureChannel(
                "EMAIL",
                "Email",
                "Deliver 6-digit login verification codes and official supervisor PDF reports via SMTP email.",
                true,
                true,
                true,
                1
        );
        ensureChannel(
                "SMS",
                "SMS",
                "Deliver mobile SMS verification codes and movement alerts (disabled until an SMS gateway is active).",
                false,
                false,
                false,
                2
        );
        ensureChannel(
                "WHATSAPP",
                "WhatsApp",
                "Deliver 6-digit login verification codes and supervisor alerts via the WhatsApp Relay Service.",
                true,
                true,
                true,
                3
        );
    }

    private void ensureChannel(String code,
                               String displayName,
                               String description,
                               boolean active,
                               boolean otpEnabled,
                               boolean supervisorAlertEnabled,
                               int displayOrder) {
        Optional<NotificationChannelConfig> existing = repository
                .findByChannelCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED);
        if (existing.isPresent()) {
            return;
        }
        NotificationChannelConfig created = NotificationChannelConfig.builder()
                .channelCode(code)
                .displayName(displayName)
                .description(description)
                .active(active)
                .otpEnabled(otpEnabled)
                .supervisorAlertEnabled(supervisorAlertEnabled)
                .displayOrder(displayOrder)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        repository.save(created);
        log.info("Initialized default NotificationChannelConfig: {} (active={})", code, active);
    }

    @Transactional(readOnly = true)
    public boolean isOtpChannelActive(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            return false;
        }
        return repository.findByChannelCodeIgnoreCaseAndRecordStatusNot(channelCode.trim(), RecordStatus.DELETED)
                .map(cfg -> cfg.isActive() && cfg.isOtpEnabled())
                .orElseGet(() -> !"SMS".equalsIgnoreCase(channelCode.trim()));
    }

    @Transactional(readOnly = true)
    public boolean isSupervisorChannelActive(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            return false;
        }
        return repository.findByChannelCodeIgnoreCaseAndRecordStatusNot(channelCode.trim(), RecordStatus.DELETED)
                .map(cfg -> cfg.isActive() && cfg.isSupervisorAlertEnabled())
                .orElseGet(() -> !"SMS".equalsIgnoreCase(channelCode.trim()));
    }

    @Transactional
    public NotificationChannelConfig updateChannel(String channelCode,
                                                   Boolean active,
                                                   Boolean otpEnabled,
                                                   Boolean supervisorAlertEnabled,
                                                   String description) {
        seedMissingDefaults();
        String code = channelCode != null ? channelCode.trim().toUpperCase() : "";
        NotificationChannelConfig entity = repository
                .findByChannelCodeIgnoreCaseAndRecordStatusNot(code, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Notification channel '" + code + "' not found"));

        boolean nextActive = active != null ? active : entity.isActive();
        boolean nextOtp = otpEnabled != null ? otpEnabled : (active != null ? active : entity.isOtpEnabled());
        boolean nextSup = supervisorAlertEnabled != null
                ? supervisorAlertEnabled
                : (active != null && !"SMS".equalsIgnoreCase(code) ? active : entity.isSupervisorAlertEnabled());

        if (!nextActive) {
            nextOtp = false;
            nextSup = false;
        }

        // Prevent disabling every OTP sign-in channel simultaneously
        List<NotificationChannelConfig> all = repository
                .findAllByRecordStatusNotOrderByDisplayOrderAsc(RecordStatus.DELETED);
        boolean anotherOtpActive = all.stream()
                .anyMatch(c -> !c.getId().equals(entity.getId()) && c.isActive() && c.isOtpEnabled());
        if ((!nextActive || !nextOtp) && !anotherOtpActive) {
            throw new BusinessRuleException("At least one OTP sign-in channel must remain active.");
        }

        entity.setActive(nextActive);
        entity.setOtpEnabled(nextOtp);
        entity.setSupervisorAlertEnabled(nextSup);
        if (description != null && !description.isBlank()) {
            entity.setDescription(description.trim());
        }

        return repository.save(entity);
    }
}

