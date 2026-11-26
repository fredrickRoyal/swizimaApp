package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.dto.AttendingOfficerDto;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.dto.ReportBackAttachmentDto;
import com.swizima.stafftrackapi.dto.ReportBackManagementActionDto;
import com.swizima.stafftrackapi.entity.OutOfOfficeAttendingOfficer;
import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;
import com.swizima.stafftrackapi.entity.ReportBackAttachment;
import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.SupervisorConfirmation;
import com.swizima.stafftrackapi.entity.enums.AttachmentKind;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.exception.BusinessRuleException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.OutOfOfficeNotificationRepository;
import com.swizima.stafftrackapi.repository.OutOfOfficeReportBackRepository;
import com.swizima.stafftrackapi.repository.SupervisorRepository;
import com.swizima.stafftrackapi.service.OutOfOfficeService;
import com.swizima.stafftrackapi.service.SupervisorNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutOfOfficeServiceImpl implements OutOfOfficeService {

    private final OutOfOfficeNotificationRepository notificationRepository;
    private final OutOfOfficeReportBackRepository reportBackRepository;
    private final SupervisorRepository supervisorRepository;
    private final SupervisorNotificationService supervisorNotificationService;

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse recordNotification(OutOfOfficeNotificationRequest request) {
        validateDates(request.startDate(), request.endDate());

        OutOfOfficeNotification notification = new OutOfOfficeNotification();
        notification.setReferenceNumber(generateReferenceNumber());
        notification.setRecordStatus(RecordStatus.ACTIVE);
        notification.setSupervisorConfirmationStatus(SupervisorConfirmationStatus.NEW);
        applyNotificationRequest(notification, request);
        attachInitialSupervisorConfirmations(notification);

        OutOfOfficeNotification saved = notificationRepository.save(notification);
        supervisorNotificationService.notifyOutOfOfficeCreated(saved);

        return OutOfOfficeNotificationResponse.from(saved);
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse updateNotification(UUID id, OutOfOfficeNotificationRequest request) {
        validateDates(request.startDate(), request.endDate());

        OutOfOfficeNotification notification = findNotification(id);
        applyNotificationRequest(notification, request);

        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse updateStatus(UUID id, OutOfOfficeStatus status) {
        OutOfOfficeNotification notification = findNotification(id);
        notification.setStatus(status);
        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse updateSupervisorConfirmation(
            UUID id, SupervisorConfirmationStatus confirmationStatus, String confirmedBy) {
        OutOfOfficeNotification notification = findNotification(id);
        SupervisorConfirmationStatus nextStatus = confirmationStatus != null
                ? confirmationStatus
                : SupervisorConfirmationStatus.SEEN;

        LocalDateTime now = nextStatus == SupervisorConfirmationStatus.SEEN ? LocalDateTime.now() : null;
        String actor = confirmedBy != null && !confirmedBy.isBlank() ? confirmedBy.trim() : "Supervisor";

        notification.setSupervisorConfirmationStatus(nextStatus);
        notification.setSupervisorSeenAt(now);
        notification.setSupervisorConfirmedBy(nextStatus == SupervisorConfirmationStatus.SEEN ? actor : null);

        if (notification.getSupervisorConfirmations() == null || notification.getSupervisorConfirmations().isEmpty()) {
            attachInitialSupervisorConfirmations(notification);
        }

        for (SupervisorConfirmation sc : notification.getSupervisorConfirmations()) {
            sc.setConfirmationStatus(nextStatus);
            sc.setSeenAt(now);
            sc.setConfirmedBy(nextStatus == SupervisorConfirmationStatus.SEEN ? actor : null);
        }

        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    private void attachInitialSupervisorConfirmations(OutOfOfficeNotification notification) {
        List<Supervisor> activeSupervisors = supervisorRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(s -> !"INACTIVE".equalsIgnoreCase(s.getStatus()))
                .toList();

        notification.getSupervisorConfirmations().clear();

        if (activeSupervisors.isEmpty()) {
            SupervisorConfirmation defaultConf = SupervisorConfirmation.builder()
                    .clientRef(notification.getClientRef())
                    .notification(notification)
                    .supervisorName(notification.getActingOfficer() != null
                            ? notification.getActingOfficer()
                            : "Unit Supervisor")
                    .confirmationStatus(SupervisorConfirmationStatus.NEW)
                    .recordStatus(RecordStatus.ACTIVE)
                    .build();
            notification.getSupervisorConfirmations().add(defaultConf);
            return;
        }

        for (Supervisor sup : activeSupervisors) {
            SupervisorConfirmation conf = SupervisorConfirmation.builder()
                    .clientRef(notification.getClientRef())
                    .notification(notification)
                    .supervisorId(sup.getId())
                    .supervisorName(sup.getName())
                    .supervisorEmail(sup.getEmail())
                    .supervisorPhone(sup.getPhone())
                    .confirmationStatus(SupervisorConfirmationStatus.NEW)
                    .recordStatus(RecordStatus.ACTIVE)
                    .build();
            notification.getSupervisorConfirmations().add(conf);
        }
    }

    @Override
    public OutOfOfficeNotificationResponse getNotification(UUID id) {
        return OutOfOfficeNotificationResponse.from(findNotification(id));
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getAllNotifications() {
        return getAllNotifications(null, null, null);
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getAllNotifications(String role,
                                                                     UUID staffMemberId,
                                                                     String officerName) {
        return notificationRepository.findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(n -> isVisibleToCaller(n, role, staffMemberId, officerName))
                .map(OutOfOfficeNotificationResponse::from)
                .toList();
    }

    @Override
    public PageResponse<OutOfOfficeNotificationResponse> searchNotifications(String query,
                                                                             String department,
                                                                             String dutyStation,
                                                                             OutOfOfficeStatus status,
                                                                             EngagementType engagementType,
                                                                             String role,
                                                                             UUID staffMemberId,
                                                                             String officerName,
                                                                             Pageable pageable) {
        if (isStaffOfficerScoped(role, staffMemberId, officerName)) {
            List<OutOfOfficeNotificationResponse> filtered = notificationRepository
                    .search(query, department, dutyStation, status, engagementType, RecordStatus.DELETED, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(n -> isVisibleToCaller(n, role, staffMemberId, officerName))
                    .map(OutOfOfficeNotificationResponse::from)
                    .toList();
            int start = (int) Math.min(pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            return PageResponse.from(new PageImpl<>(filtered.subList(start, end), pageable, filtered.size()));
        }

        return PageResponse.from(
                notificationRepository.search(query, department, dutyStation, status, engagementType, RecordStatus.DELETED, pageable)
                        .map(OutOfOfficeNotificationResponse::from)
        );
    }

    private boolean isStaffOfficerScoped(String role, UUID staffMemberId, String officerName) {
        boolean hasIdentity = staffMemberId != null || (officerName != null && !officerName.isBlank());
        if (!hasIdentity) {
            return false;
        }
        if (role == null || role.isBlank()) {
            return true;
        }
        String normRole = role.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        return "STAFF_OFFICER".equals(normRole);
    }

    /**
     * When the caller is a Staff Officer, returns true only if the notification was created by them
     * ({@code staffMemberId} or {@code officerName}).
     */
    private boolean isVisibleToCaller(OutOfOfficeNotification n,
                                      String role,
                                      UUID staffMemberId,
                                      String officerName) {
        if (!isStaffOfficerScoped(role, staffMemberId, officerName)) {
            return true;
        }

        // Only return records created by the logged-in Staff Officer
        if (staffMemberId != null && staffMemberId.equals(n.getStaffMemberId())) {
            return true;
        }
        return namesMatch(officerName, n.getOfficerName());
    }

    private boolean namesMatch(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return false;
        }
        String cleanA = a.trim().toLowerCase(Locale.ROOT);
        String cleanB = b.trim().toLowerCase(Locale.ROOT);
        if (cleanA.equals(cleanB)) {
            return true;
        }
        Set<String> tokensA = Arrays.stream(cleanA.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        Set<String> tokensB = Arrays.stream(cleanB.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return !tokensA.isEmpty() && tokensA.equals(tokensB);
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse submitReportBack(UUID notificationId, OutOfOfficeReportBackRequest request) {
        OutOfOfficeNotification notification = findNotification(notificationId);

        OutOfOfficeReportBack reportBack = notification.getReportBack();
        if (reportBack == null) {
            reportBack = new OutOfOfficeReportBack();
            reportBack.setNotification(notification);
            reportBack.setClientRef(notification.getClientRef());
            reportBack.setRecordStatus(RecordStatus.ACTIVE);
            notification.setReportBack(reportBack);
        }

        List<String> cleanIssues = request.keyIssuesList() != null
                ? request.keyIssuesList().stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        List<String> cleanAchievements = request.achievementsList() != null
                ? request.achievementsList().stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        String joinedIssues = !cleanIssues.isEmpty()
                ? String.join("; ", cleanIssues)
                : (request.outstandingIssues() != null ? request.outstandingIssues() : "");
        String joinedAchievements = !cleanAchievements.isEmpty()
                ? String.join("; ", cleanAchievements)
                : (request.whatWasAchieved() != null ? request.whatWasAchieved() : "Completed as scheduled");

        reportBack.setActualReturnDate(request.actualReturnDate());
        reportBack.setStakeholdersMet(request.stakeholdersMet());
        reportBack.setWhatWasDone(request.whatWasDone() != null && !request.whatWasDone().isBlank()
                ? request.whatWasDone()
                : joinedAchievements);
        reportBack.setWhatWasDiscussed(request.whatWasDiscussed() != null && !request.whatWasDiscussed().isBlank()
                ? request.whatWasDiscussed()
                : joinedIssues);
        reportBack.setWhatWasAchieved(joinedAchievements);
        reportBack.setOutstandingIssues(joinedIssues);
        reportBack.setKeyIssuesList(new ArrayList<>(cleanIssues));
        reportBack.setAchievementsList(new ArrayList<>(cleanAchievements));
        reportBack.setSubmittedAt(LocalDate.now());

        // Replace management actions
        reportBack.getManagementActions().clear();
        List<String> actionSummaries = new ArrayList<>();
        if (request.managementActions() != null) {
            for (ReportBackManagementActionDto dto : request.managementActions()) {
                if (dto.followUpAction() == null || dto.followUpAction().isBlank()) {
                    continue;
                }
                ReportBackManagementAction action = ReportBackManagementAction.builder()
                        .clientRef(notification.getClientRef())
                        .reportBack(reportBack)
                        .followUpAction(dto.followUpAction().trim())
                        .responsibleStaffId(dto.responsibleStaffId())
                        .responsibleOfficer(dto.responsibleOfficer() != null ? dto.responsibleOfficer().trim() : "Assigned Officer")
                        .targetDate(dto.targetDate())
                        .recordStatus(RecordStatus.ACTIVE)
                        .build();
                reportBack.getManagementActions().add(action);
                actionSummaries.add(action.getFollowUpAction() + " (" + action.getResponsibleOfficer()
                        + (action.getTargetDate() != null ? " by " + action.getTargetDate() : "") + ")");
            }
        }

        // Replace attachments
        reportBack.getAttachments().clear();
        if (request.attachments() != null) {
            for (ReportBackAttachmentDto attDto : request.attachments()) {
                if (attDto.name() == null || attDto.name().isBlank()) {
                    continue;
                }
                ReportBackAttachment att = ReportBackAttachment.builder()
                        .clientRef(notification.getClientRef())
                        .reportBack(reportBack)
                        .name(attDto.name().trim())
                        .mimeType(attDto.mimeType())
                        .sizeBytes(attDto.sizeBytes())
                        .kind(attDto.kind() != null ? attDto.kind() : AttachmentKind.DOCUMENT)
                        .dataUrl(attDto.dataUrl())
                        .uploadedAt(attDto.uploadedAt() != null ? attDto.uploadedAt() : LocalDate.now())
                        .recordStatus(RecordStatus.ACTIVE)
                        .build();
                reportBack.getAttachments().add(att);
            }
        }

        notification.setStatus(OutOfOfficeStatus.RETURNED);
        if (!actionSummaries.isEmpty()) {
            notification.setManagementActionRequired(String.join("; ", actionSummaries));
        }

        OutOfOfficeNotification saved = notificationRepository.save(notification);
        supervisorNotificationService.notifyReportBackSubmitted(saved, saved.getReportBack());

        return OutOfOfficeNotificationResponse.from(saved);
    }

    @Override
    public OutOfOfficeReportBackResponse getReportBack(UUID notificationId) {
        findNotification(notificationId);
        OutOfOfficeReportBack rb = reportBackRepository
                .findByNotificationIdAndRecordStatusNot(notificationId, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No report-back submitted yet for Out-of-Office Notification '" + notificationId + "'"));
        return OutOfOfficeReportBackResponse.from(rb);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID id) {
        OutOfOfficeNotification notification = findNotification(id);
        notification.setRecordStatus(RecordStatus.DELETED);
        if (notification.getReportBack() != null) {
            notification.getReportBack().setRecordStatus(RecordStatus.DELETED);
        }
        notificationRepository.save(notification);
    }

    // ------------------------------------------------------------------ helpers

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("endDate cannot be earlier than startDate");
        }
    }

    private void applyNotificationRequest(OutOfOfficeNotification n, OutOfOfficeNotificationRequest r) {
        if (r.clientRef() != null) {
            n.setClientRef(r.clientRef());
        }
        n.setStaffMemberId(r.staffMemberId());
        n.setOfficerName(r.officerName().trim());
        n.setOfficerRole(r.officerRole().trim());
        n.setDepartment(r.department().trim());
        n.setDutyStation(r.dutyStation().trim());
        n.setEngagementType(r.engagementType());
        n.setDestination(r.destination().trim());
        n.setHostOrganization(r.hostOrganization().trim());
        n.setStartDate(r.startDate());
        n.setEndDate(r.endDate());

        List<String> cleanPurposes = r.purposeList() != null
                ? r.purposeList().stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        String joinedPurpose = !cleanPurposes.isEmpty()
                ? String.join("; ", cleanPurposes)
                : (r.purpose() != null && !r.purpose().isBlank() ? r.purpose().trim() : "Official Duty Engagement");

        n.setPurpose(joinedPurpose);
        n.setPurposeList(new ArrayList<>(!cleanPurposes.isEmpty() ? cleanPurposes : List.of(joinedPurpose)));

        boolean attendingOthers = Boolean.TRUE.equals(r.attendingWithOthers());
        n.setAttendingWithOthers(attendingOthers);
        n.getAttendingOfficers().clear();
        if (attendingOthers && r.attendingOfficers() != null) {
            for (AttendingOfficerDto ao : r.attendingOfficers()) {
                if (ao.name() == null || ao.name().isBlank()) {
                    continue;
                }
                OutOfOfficeAttendingOfficer entity = OutOfOfficeAttendingOfficer.builder()
                        .clientRef(n.getClientRef())
                        .notification(n)
                        .staffMemberId(ao.staffMemberId())
                        .name(ao.name().trim())
                        .designation(ao.designation())
                        .department(ao.department() != null ? ao.department() : n.getDepartment())
                        .dutyStation(ao.dutyStation() != null ? ao.dutyStation() : n.getDutyStation())
                        .custom(Boolean.TRUE.equals(ao.isCustom()))
                        .recordStatus(RecordStatus.ACTIVE)
                        .build();
                n.getAttendingOfficers().add(entity);
            }
        }

        n.setActingOfficer(r.actingOfficer() != null && !r.actingOfficer().isBlank()
                ? r.actingOfficer().trim()
                : "Assigned by Unit Supervisor");
        n.setContactPhone(r.contactPhone());
        if (r.status() != null) {
            n.setStatus(r.status());
        } else if (n.getStatus() == null) {
            n.setStatus(OutOfOfficeStatus.ACTIVE_AWAY);
        }
        if (r.managementActionRequired() != null) {
            n.setManagementActionRequired(r.managementActionRequired());
        }
    }

    private OutOfOfficeNotification findNotification(UUID id) {
        return notificationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Out-of-Office Notification with id '" + id + "' not found"));
    }

    private String generateReferenceNumber() {
        long nextNum = notificationRepository.count() + 101;
        return "OOO-" + nextNum;
    }
}

