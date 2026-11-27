package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OutOfOfficeService {

    OutOfOfficeNotificationResponse recordNotification(OutOfOfficeNotificationRequest request);

    OutOfOfficeNotificationResponse updateNotification(UUID id, OutOfOfficeNotificationRequest request);

    OutOfOfficeNotificationResponse updateStatus(UUID id, OutOfOfficeStatus status);

    OutOfOfficeNotificationResponse reviewByUnitHead(
            UUID id, UnitHeadApprovalStatus decision, String reviewedBy, String comment);

    OutOfOfficeNotificationResponse updateSupervisorConfirmation(
            UUID id, SupervisorConfirmationStatus confirmationStatus, String confirmedBy);

    OutOfOfficeNotificationResponse getNotification(UUID id);

    List<OutOfOfficeNotificationResponse> getAllNotifications();

    List<OutOfOfficeNotificationResponse> getAllNotifications(String role, UUID staffMemberId, String officerName);

    List<OutOfOfficeNotificationResponse> getAllNotifications(
            String role, UUID staffMemberId, String officerName, String callerEmail, String callerPhone);

    PageResponse<OutOfOfficeNotificationResponse> searchNotifications(String query,
                                                                      String department,
                                                                      String dutyStation,
                                                                      OutOfOfficeStatus status,
                                                                      EngagementType engagementType,
                                                                      String role,
                                                                      UUID staffMemberId,
                                                                      String officerName,
                                                                      Pageable pageable);

    PageResponse<OutOfOfficeNotificationResponse> searchNotifications(String query,
                                                                      String department,
                                                                      String dutyStation,
                                                                      OutOfOfficeStatus status,
                                                                      EngagementType engagementType,
                                                                      String role,
                                                                      UUID staffMemberId,
                                                                      String officerName,
                                                                      String callerEmail,
                                                                      String callerPhone,
                                                                      Pageable pageable);

    OutOfOfficeNotificationResponse submitReportBack(UUID notificationId, OutOfOfficeReportBackRequest request);

    OutOfOfficeReportBackResponse getReportBack(UUID notificationId);

    void deleteNotification(UUID id);
}

