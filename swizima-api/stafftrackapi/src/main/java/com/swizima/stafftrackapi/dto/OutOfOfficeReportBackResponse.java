package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OutOfOfficeReportBackResponse(
        UUID id,
        UUID notificationId,
        LocalDate actualReturnDate,
        String stakeholdersMet,
        String whatWasDone,
        String whatWasDiscussed,
        String whatWasAchieved,
        String outstandingIssues,
        List<String> keyIssuesList,
        List<String> achievementsList,
        List<ReportBackManagementActionDto> managementActions,
        List<ReportBackAttachmentDto> attachments,
        LocalDate submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutOfOfficeReportBackResponse from(OutOfOfficeReportBack rb) {
        if (rb == null) {
            return null;
        }
        return new OutOfOfficeReportBackResponse(
                rb.getId(),
                rb.getNotification() != null ? rb.getNotification().getId() : null,
                rb.getActualReturnDate(),
                rb.getStakeholdersMet(),
                rb.getWhatWasDone(),
                rb.getWhatWasDiscussed(),
                rb.getWhatWasAchieved(),
                rb.getOutstandingIssues(),
                rb.getKeyIssuesList() != null ? List.copyOf(rb.getKeyIssuesList()) : List.of(),
                rb.getAchievementsList() != null ? List.copyOf(rb.getAchievementsList()) : List.of(),
                rb.getManagementActions() != null
                        ? rb.getManagementActions().stream().map(ReportBackManagementActionDto::from).toList()
                        : List.of(),
                rb.getAttachments() != null
                        ? rb.getAttachments().stream().map(ReportBackAttachmentDto::from).toList()
                        : List.of(),
                rb.getSubmittedAt(),
                rb.getCreatedAt(),
                rb.getUpdatedAt()
        );
    }
}

