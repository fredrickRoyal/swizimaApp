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
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
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

        boolean submitterIsUnitOrDeptHead = isDepartmentOrUnitHeadRole(request.submitterRole(), request.officerRole());
        if (submitterIsUnitOrDeptHead) {
            notification.setUnitHeadSupervisorId(null);
            notification.setUnitHeadName(null);
            notification.setUnitHeadEmail(null);
            notification.setUnitHeadPhone(null);
            notification.setUnitHeadApprovalStatus(UnitHeadApprovalStatus.NOT_REQUIRED);
            boolean isFuture = notification.getStartDate() != null
                    && notification.getStartDate().isAfter(LocalDate.now());
            notification.setStatus(isFuture ? OutOfOfficeStatus.APPROVED : OutOfOfficeStatus.ACTIVE_AWAY);
            attachOverallHeadConfirmations(notification, true);
        } else if (hasUnitHeadAssigned(notification)) {
            notification.setUnitHeadApprovalStatus(UnitHeadApprovalStatus.PENDING_UNIT_HEAD);
            notification.setStatus(OutOfOfficeStatus.PENDING_APPROVAL);
            attachUnitHeadConfirmationOnly(notification);
        } else {
            notification.setUnitHeadApprovalStatus(UnitHeadApprovalStatus.NOT_REQUIRED);
            attachOverallHeadConfirmations(notification, true);
        }

        OutOfOfficeNotification saved = notificationRepository.save(notification);
        supervisorNotificationService.notifyOutOfOfficeCreated(saved);

        return OutOfOfficeNotificationResponse.from(saved);
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse updateNotification(UUID id, OutOfOfficeNotificationRequest request) {
        validateDates(request.startDate(), request.endDate());

        OutOfOfficeNotification notification = findNotification(id);
        OutOfOfficeStatus prevStatus = notification.getStatus();
        UnitHeadApprovalStatus prevApproval = notification.getUnitHeadApprovalStatus();

        applyNotificationRequest(notification, request);

        if (prevApproval == UnitHeadApprovalStatus.PENDING_UNIT_HEAD
                && prevStatus == OutOfOfficeStatus.PENDING_APPROVAL) {
            notification.setStatus(OutOfOfficeStatus.PENDING_APPROVAL);
            notification.setUnitHeadApprovalStatus(UnitHeadApprovalStatus.PENDING_UNIT_HEAD);
        } else if (notification.getUnitHeadApprovalStatus() == null) {
            notification.setUnitHeadApprovalStatus(
                    hasUnitHeadAssigned(notification)
                            ? UnitHeadApprovalStatus.PENDING_UNIT_HEAD
                            : UnitHeadApprovalStatus.NOT_REQUIRED
            );
        }

        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse updateStatus(UUID id, OutOfOfficeStatus status) {
        OutOfOfficeNotification notification = findNotification(id);
        if (notification.getUnitHeadApprovalStatus() == UnitHeadApprovalStatus.PENDING_UNIT_HEAD
                && (status == OutOfOfficeStatus.APPROVED || status == OutOfOfficeStatus.ACTIVE_AWAY)) {
            return reviewByUnitHead(id, UnitHeadApprovalStatus.ACCEPTED, notification.getUnitHeadName(), null);
        }
        notification.setStatus(status);
        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public OutOfOfficeNotificationResponse reviewByUnitHead(
            UUID id, UnitHeadApprovalStatus decision, String reviewedBy, String comment) {
        if (decision != UnitHeadApprovalStatus.ACCEPTED && decision != UnitHeadApprovalStatus.REJECTED) {
            throw new BusinessRuleException("Unit Head decision must be ACCEPTED or REJECTED.");
        }

        OutOfOfficeNotification notification = findNotification(id);
        LocalDateTime now = LocalDateTime.now();
        String actor = firstNonBlank(reviewedBy, firstNonBlank(notification.getUnitHeadName(), "Unit Head"));

        notification.setUnitHeadApprovalStatus(decision);
        notification.setUnitHeadReviewedAt(now);
        notification.setUnitHeadReviewedBy(actor);
        notification.setUnitHeadComment(comment != null && !comment.isBlank() ? comment.trim() : null);

        if (notification.getSupervisorConfirmations() != null) {
            for (SupervisorConfirmation sc : notification.getSupervisorConfirmations()) {
                if ("UNIT_HEAD".equalsIgnoreCase(sc.getSupervisorCategory())
                        || sc.getSupervisorCategory() == null) {
                    sc.setConfirmationStatus(SupervisorConfirmationStatus.SEEN);
                    sc.setSeenAt(now);
                    sc.setConfirmedBy(actor);
                }
            }
        }

        if (decision == UnitHeadApprovalStatus.ACCEPTED) {
            if (notification.getStatus() != OutOfOfficeStatus.RETURNED) {
                boolean isFuture = notification.getStartDate() != null
                        && notification.getStartDate().isAfter(LocalDate.now());
                notification.setStatus(isFuture ? OutOfOfficeStatus.APPROVED : OutOfOfficeStatus.ACTIVE_AWAY);
            }
            int addedOverall = attachOverallHeadConfirmations(notification, false);
            if (addedOverall > 0) {
                notification.setSupervisorConfirmationStatus(SupervisorConfirmationStatus.NEW);
                notification.setSupervisorSeenAt(null);
                notification.setSupervisorConfirmedBy(null);
            } else {
                notification.setSupervisorConfirmationStatus(SupervisorConfirmationStatus.SEEN);
                notification.setSupervisorSeenAt(now);
                notification.setSupervisorConfirmedBy(actor);
            }

            OutOfOfficeNotification saved = notificationRepository.save(notification);
            supervisorNotificationService.notifyOverallHeadsAfterUnitHeadAcceptance(saved);
            return OutOfOfficeNotificationResponse.from(saved);
        }

        notification.setStatus(OutOfOfficeStatus.CANCELLED);
        notification.setSupervisorConfirmationStatus(SupervisorConfirmationStatus.SEEN);
        notification.setSupervisorSeenAt(now);
        notification.setSupervisorConfirmedBy(actor);
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
            if (hasUnitHeadAssigned(notification)
                    && notification.getUnitHeadApprovalStatus() == UnitHeadApprovalStatus.PENDING_UNIT_HEAD) {
                attachUnitHeadConfirmationOnly(notification);
            } else {
                attachOverallHeadConfirmations(notification, true);
            }
        }

        for (SupervisorConfirmation sc : notification.getSupervisorConfirmations()) {
            sc.setConfirmationStatus(nextStatus);
            sc.setSeenAt(now);
            sc.setConfirmedBy(nextStatus == SupervisorConfirmationStatus.SEEN ? actor : null);
        }

        return OutOfOfficeNotificationResponse.from(notificationRepository.save(notification));
    }

    private boolean hasUnitHeadAssigned(OutOfOfficeNotification n) {
        return n.getUnitHeadSupervisorId() != null
                || (n.getUnitHeadName() != null && !n.getUnitHeadName().isBlank())
                || (n.getUnitHeadEmail() != null && !n.getUnitHeadEmail().isBlank())
                || (n.getUnitHeadPhone() != null && !n.getUnitHeadPhone().isBlank());
    }

    private boolean isDepartmentOrUnitHeadRole(String submitterRole, String officerRole) {
        return matchesDeptOrUnitHead(submitterRole) || matchesDeptOrUnitHead(officerRole);
    }

    private boolean matchesDeptOrUnitHead(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s/\\-]+", "_");
        return "DEPARTMENT_HEAD".equals(norm)
                || "UNIT_HEAD".equals(norm)
                || "DEPARTMENT_UNIT_HEAD".equals(norm);
    }

    private void attachUnitHeadConfirmationOnly(OutOfOfficeNotification notification) {
        notification.getSupervisorConfirmations().clear();
        SupervisorConfirmation conf = SupervisorConfirmation.builder()
                .clientRef(notification.getClientRef())
                .notification(notification)
                .supervisorId(notification.getUnitHeadSupervisorId())
                .supervisorName(firstNonBlank(notification.getUnitHeadName(), "Unit Head"))
                .supervisorEmail(notification.getUnitHeadEmail())
                .supervisorPhone(notification.getUnitHeadPhone())
                .supervisorCategory("UNIT_HEAD")
                .confirmationStatus(SupervisorConfirmationStatus.NEW)
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        notification.getSupervisorConfirmations().add(conf);
    }

    private int attachOverallHeadConfirmations(OutOfOfficeNotification notification, boolean clearExisting) {
        List<Supervisor> overallSupervisors = supervisorRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(s -> !"INACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> {
                    String cat = s.getCategory();
                    return cat == null || cat.isBlank() || "OVERALL_HEAD".equalsIgnoreCase(cat.trim());
                })
                .toList();

        if (clearExisting) {
            notification.getSupervisorConfirmations().clear();
        }

        if (overallSupervisors.isEmpty()) {
            if (clearExisting) {
                SupervisorConfirmation defaultConf = SupervisorConfirmation.builder()
                        .clientRef(notification.getClientRef())
                        .notification(notification)
                        .supervisorName(notification.getActingOfficer() != null
                                ? notification.getActingOfficer()
                                : "Overall Head")
                        .supervisorCategory("OVERALL_HEAD")
                        .confirmationStatus(SupervisorConfirmationStatus.NEW)
                        .recordStatus(RecordStatus.ACTIVE)
                        .build();
                notification.getSupervisorConfirmations().add(defaultConf);
            }
            return 0;
        }

        int added = 0;
        for (Supervisor sup : overallSupervisors) {
            boolean alreadyPresent = notification.getSupervisorConfirmations().stream()
                    .anyMatch(existing -> sup.getId() != null && sup.getId().equals(existing.getSupervisorId())
                            && "OVERALL_HEAD".equalsIgnoreCase(existing.getSupervisorCategory()));
            if (alreadyPresent) {
                continue;
            }
            SupervisorConfirmation conf = SupervisorConfirmation.builder()
                    .clientRef(notification.getClientRef())
                    .notification(notification)
                    .supervisorId(sup.getId())
                    .supervisorName(sup.getName())
                    .supervisorEmail(sup.getEmail())
                    .supervisorPhone(sup.getPhone())
                    .supervisorCategory("OVERALL_HEAD")
                    .confirmationStatus(SupervisorConfirmationStatus.NEW)
                    .recordStatus(RecordStatus.ACTIVE)
                    .build();
            notification.getSupervisorConfirmations().add(conf);
            added++;
        }
        return added;
    }

    @Override
    public OutOfOfficeNotificationResponse getNotification(UUID id) {
        return OutOfOfficeNotificationResponse.from(findNotification(id));
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getAllNotifications() {
        return getAllNotifications(null, null, null, null, null);
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getAllNotifications(String role,
                                                                     UUID staffMemberId,
                                                                     String officerName) {
        return getAllNotifications(role, staffMemberId, officerName, null, null);
    }

    @Override
    public List<OutOfOfficeNotificationResponse> getAllNotifications(String role,
                                                                     UUID staffMemberId,
                                                                     String officerName,
                                                                     String callerEmail,
                                                                     String callerPhone) {
        return notificationRepository.findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(n -> isVisibleToCaller(n, role, staffMemberId, officerName, callerEmail, callerPhone))
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
        return searchNotifications(
                query, department, dutyStation, status, engagementType,
                role, staffMemberId, officerName, null, null, pageable);
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
                                                                             String callerEmail,
                                                                             String callerPhone,
                                                                             Pageable pageable) {
        if (isStaffOfficerScoped(role, staffMemberId, officerName, callerEmail, callerPhone)) {
            List<OutOfOfficeNotificationResponse> filtered = notificationRepository
                    .search(query, department, dutyStation, status, engagementType, RecordStatus.DELETED, Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(n -> isVisibleToCaller(n, role, staffMemberId, officerName, callerEmail, callerPhone))
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

    private boolean isStaffOfficerScoped(String role,
                                         UUID staffMemberId,
                                         String officerName,
                                         String callerEmail,
                                         String callerPhone) {
        boolean hasIdentity = staffMemberId != null
                || (officerName != null && !officerName.isBlank())
                || (callerEmail != null && !callerEmail.isBlank())
                || (callerPhone != null && !callerPhone.isBlank());
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
     * When the caller is a Staff Officer, returns true if the notification was created by them
     * OR if they were selected as the Unit Head for that notification.
     */
    private boolean isVisibleToCaller(OutOfOfficeNotification n,
                                      String role,
                                      UUID staffMemberId,
                                      String officerName,
                                      String callerEmail,
                                      String callerPhone) {
        if (!isStaffOfficerScoped(role, staffMemberId, officerName, callerEmail, callerPhone)) {
            return true;
        }

        if (staffMemberId != null && staffMemberId.equals(n.getStaffMemberId())) {
            return true;
        }
        if (namesMatch(officerName, n.getOfficerName())) {
            return true;
        }
        if (namesMatch(officerName, n.getUnitHeadName())) {
            return true;
        }
        if (callerEmail != null && !callerEmail.isBlank()
                && n.getUnitHeadEmail() != null
                && callerEmail.trim().equalsIgnoreCase(n.getUnitHeadEmail().trim())) {
            return true;
        }
        String digitsCaller = normalizeDigits(callerPhone);
        String digitsUnitHead = normalizeDigits(n.getUnitHeadPhone());
        return !digitsCaller.isEmpty() && digitsCaller.equals(digitsUnitHead);
    }

    private static String normalizeDigits(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("0") && d.length() == 10) {
            return "256" + d.substring(1);
        }
        return d;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
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

        if (r.unitHeadSupervisorId() != null) {
            n.setUnitHeadSupervisorId(r.unitHeadSupervisorId());
            Supervisor sup = supervisorRepository
                    .findByIdAndRecordStatusNot(r.unitHeadSupervisorId(), RecordStatus.DELETED)
                    .orElse(null);
            n.setUnitHeadName(firstNonBlank(r.unitHeadName(), sup != null ? sup.getName() : null));
            n.setUnitHeadEmail(firstNonBlank(r.unitHeadEmail(), sup != null ? sup.getEmail() : null));
            n.setUnitHeadPhone(firstNonBlank(r.unitHeadPhone(), sup != null ? sup.getPhone() : null));
        } else if ((r.unitHeadName() != null && !r.unitHeadName().isBlank())
                || (r.unitHeadEmail() != null && !r.unitHeadEmail().isBlank())
                || (r.unitHeadPhone() != null && !r.unitHeadPhone().isBlank())) {
            n.setUnitHeadSupervisorId(null);
            n.setUnitHeadName(r.unitHeadName() != null && !r.unitHeadName().isBlank() ? r.unitHeadName().trim() : null);
            n.setUnitHeadEmail(r.unitHeadEmail() != null && !r.unitHeadEmail().isBlank() ? r.unitHeadEmail().trim().toLowerCase(Locale.ROOT) : null);
            n.setUnitHeadPhone(r.unitHeadPhone() != null && !r.unitHeadPhone().isBlank() ? r.unitHeadPhone().trim() : null);
        }

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

