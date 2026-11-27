package com.swizima.stafftrackapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for submitting or updating a Report Back on an Out-of-Office Engagement (Section B).
 */
public record OutOfOfficeReportBackRequest(
        @NotNull(message = "actualReturnDate is required")
        LocalDate actualReturnDate,

        String stakeholdersMet,

        String whatWasDone,

        String whatWasDiscussed,

        String whatWasAchieved,

        String outstandingIssues,

        List<String> keyIssuesList,

        List<String> achievementsList,

        List<@Valid ReportBackManagementActionDto> managementActions,

        List<@Valid ReportBackAttachmentDto> attachments
) {
}

