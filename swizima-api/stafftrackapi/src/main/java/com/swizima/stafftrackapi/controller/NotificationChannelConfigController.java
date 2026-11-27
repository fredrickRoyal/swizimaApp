package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.entity.NotificationChannelConfig;
import com.swizima.stafftrackapi.service.NotificationChannelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack/channel-configs")
@RequiredArgsConstructor
@Tag(name = "Notification & OTP Channels", description = "Configure which delivery channels (EMAIL, SMS, WHATSAPP) are active")
public class NotificationChannelConfigController {

    private final NotificationChannelConfigService channelConfigService;

    public record ChannelConfigDto(
            UUID id,
            String channelCode,
            String displayName,
            String description,
            boolean active,
            boolean otpEnabled,
            boolean supervisorAlertEnabled,
            int displayOrder,
            String updatedAt
    ) {
        public static ChannelConfigDto from(NotificationChannelConfig c) {
            String updated = c.getUpdatedAt() != null
                    ? c.getUpdatedAt().toLocalDate().toString()
                    : (c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString());
            return new ChannelConfigDto(
                    c.getId(),
                    c.getChannelCode(),
                    c.getDisplayName(),
                    c.getDescription(),
                    c.isActive(),
                    c.isOtpEnabled(),
                    c.isSupervisorAlertEnabled(),
                    c.getDisplayOrder(),
                    updated
            );
        }
    }

    public record ChannelConfigUpdateRequest(
            Boolean active,
            Boolean otpEnabled,
            Boolean supervisorAlertEnabled,
            String description
    ) {
    }

    @GetMapping({"/all", ""})
    @Operation(summary = "List all notification & OTP delivery channel configurations")
    public ResponseEntity<List<ChannelConfigDto>> getAllChannels() {
        List<ChannelConfigDto> list = channelConfigService.ensureDefaultsAndList()
                .stream()
                .map(ChannelConfigDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{channelCode}")
    @Operation(summary = "Update active / OTP / supervisor-alert flags for a channel (EMAIL, SMS, WHATSAPP)")
    public ResponseEntity<ChannelConfigDto> patchChannel(
            @PathVariable String channelCode,
            @RequestBody ChannelConfigUpdateRequest request) {
        NotificationChannelConfig updated = channelConfigService.updateChannel(
                channelCode,
                request.active(),
                request.otpEnabled(),
                request.supervisorAlertEnabled(),
                request.description()
        );
        return ResponseEntity.ok(ChannelConfigDto.from(updated));
    }

    @PutMapping("/{channelCode}")
    @Operation(summary = "Update a notification channel configuration by channelCode")
    public ResponseEntity<ChannelConfigDto> putChannel(
            @PathVariable String channelCode,
            @RequestBody ChannelConfigUpdateRequest request) {
        return patchChannel(channelCode, request);
    }
}

