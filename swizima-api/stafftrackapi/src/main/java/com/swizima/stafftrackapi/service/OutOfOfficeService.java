package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OutOfOfficeService {

    OutOfOfficeNotificationResponse recordNotification(OutOfOfficeNotificationRequest request);

    OutOfOfficeNotificationResponse updateNotification(UUID id, OutOfOfficeNotificationRequest request);

    OutOfOfficeNotificationResponse updateStatus(UUID id, OutOfOfficeStatus status);

    OutOfOfficeNotificationResponse updateSupervisorConfirmation(
            UUID id, SupervisorConfirmationStatus confirmationStatus, String confirmedBy);

    OutOfOfficeNotificationResponse getNotification(UUID id);

    List<OutOfOfficeNotificationResponse> getAllNotifications();

    List<OutOfOfficeNotificationResponse> getAllNotifications(String role, UUID staffMemberId, String officerName);

    PageResponse<OutOfOfficeNotificationResponse> searchNotifications(String query,
                                                                      String department,
                                                                      String dutyStation,
                                                                      OutOfOfficeStatus status,
                                                                      EngagementType engagementType,
                                                                      String role,
                                                                      UUID staffMemberId,
                                                                      String officerName,
                                                                      Pageable pageable);

    OutOfOfficeNotificationResponse submitReportBack(UUID notificationId, OutOfOfficeReportBackRequest request);

    OutOfOfficeReportBackResponse getReportBack(UUID notificationId);

    void deleteNotification(UUID id);
}

