package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeNotificationResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackRequest;
import com.swizima.stafftrackapi.dto.OutOfOfficeReportBackResponse;
import com.swizima.stafftrackapi.dto.OutOfOfficeStatusRequest;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.dto.UnitHeadDecisionRequest;
import com.swizima.stafftrackapi.entity.enums.EngagementType;
import com.swizima.stafftrackapi.entity.enums.OutOfOfficeStatus;
import com.swizima.stafftrackapi.entity.enums.SupervisorConfirmationStatus;
import com.swizima.stafftrackapi.service.OutOfOfficeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack/out-of-office")
@RequiredArgsConstructor
@Tag(name = "Out-of-Office & Report Back", description = "Record Out-of-Office Notifications (Section A) and Submit Report Back on Out-of-Office Engagements (Section B)")
public class OutOfOfficeController {

    private final OutOfOfficeService outOfOfficeService;

    @PostMapping
    @Operation(summary = "Record a new Out-of-Office Notification (Section A)")
    public ResponseEntity<OutOfOfficeNotificationResponse> recordNotification(
            @Valid @RequestBody OutOfOfficeNotificationRequest request) {
        OutOfOfficeNotificationResponse created = outOfOfficeService.recordNotification(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search and paginate Out-of-Office Notifications (supports Staff Officer scoping by creator or assigned Unit Head)")
    public ResponseEntity<PageResponse<OutOfOfficeNotificationResponse>> searchNotifications(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String dutyStation,
            @RequestParam(required = false) OutOfOfficeStatus status,
            @RequestParam(required = false) EngagementType engagementType,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UUID staffMemberId,
            @RequestParam(required = false) String officerName,
            @RequestParam(required = false) String callerEmail,
            @RequestParam(required = false) String callerPhone,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(outOfOfficeService.searchNotifications(
                query, department, dutyStation, status, engagementType,
                role, staffMemberId, officerName, callerEmail, callerPhone, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "List active Out-of-Office Notifications (when role=STAFF_OFFICER, returns records created by them or assigned to them as Unit Head)")
    public ResponseEntity<List<OutOfOfficeNotificationResponse>> getAllNotifications(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UUID staffMemberId,
            @RequestParam(required = false) String officerName,
            @RequestParam(required = false) String callerEmail,
            @RequestParam(required = false) String callerPhone) {
        return ResponseEntity.ok(outOfOfficeService.getAllNotifications(
                role, staffMemberId, officerName, callerEmail, callerPhone));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an Out-of-Office Notification (including Section B Report Back if submitted)")
    public ResponseEntity<OutOfOfficeNotificationResponse> getNotification(@PathVariable UUID id) {
        return ResponseEntity.ok(outOfOfficeService.getNotification(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an Out-of-Office Notification (Section A)")
    public ResponseEntity<OutOfOfficeNotificationResponse> updateNotification(
            @PathVariable UUID id,
            @Valid @RequestBody OutOfOfficeNotificationRequest request) {
        return ResponseEntity.ok(outOfOfficeService.updateNotification(id, request));
    }

    @RequestMapping(value = "/{id}/status", method = {org.springframework.web.bind.annotation.RequestMethod.PATCH, org.springframework.web.bind.annotation.RequestMethod.PUT})
    @Operation(summary = "Update Out-of-Office Notification status (e.g. ACTIVE_AWAY, RETURNED)")
    public ResponseEntity<OutOfOfficeNotificationResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OutOfOfficeStatusRequest request) {
        return ResponseEntity.ok(outOfOfficeService.updateStatus(id, request.status()));
    }

    @RequestMapping(value = "/{id}/unit-head-decision", method = {org.springframework.web.bind.annotation.RequestMethod.PUT, org.springframework.web.bind.annotation.RequestMethod.PATCH})
    @Operation(summary = "Unit Head accepts or rejects an Out-of-Office Notification; accepting automatically notifies Overall Heads")
    public ResponseEntity<OutOfOfficeNotificationResponse> submitUnitHeadDecision(
            @PathVariable UUID id,
            @Valid @RequestBody UnitHeadDecisionRequest request) {
        return ResponseEntity.ok(outOfOfficeService.reviewByUnitHead(
                id, request.decision(), request.reviewedBy(), request.comment()));
    }

    @PostMapping("/{id}/report-back")
    @Operation(summary = "Submit or update Report Back on Out-of-Office Engagement (Section B)")
    public ResponseEntity<OutOfOfficeNotificationResponse> submitReportBack(
            @PathVariable UUID id,
            @Valid @RequestBody OutOfOfficeReportBackRequest request) {
        return ResponseEntity.ok(outOfOfficeService.submitReportBack(id, request));
    }

    @PutMapping("/{id}/report-back")
    @Operation(summary = "Update Report Back on Out-of-Office Engagement (Section B)")
    public ResponseEntity<OutOfOfficeNotificationResponse> updateReportBack(
            @PathVariable UUID id,
            @Valid @RequestBody OutOfOfficeReportBackRequest request) {
        return ResponseEntity.ok(outOfOfficeService.submitReportBack(id, request));
    }

    @GetMapping("/{id}/report-back")
    @Operation(summary = "Get the Report Back (Section B) for an Out-of-Office Notification")
    public ResponseEntity<OutOfOfficeReportBackResponse> getReportBack(@PathVariable UUID id) {
        return ResponseEntity.ok(outOfOfficeService.getReportBack(id));
    }

    public record SupervisorConfirmationRequest(
            SupervisorConfirmationStatus confirmationStatus,
            String confirmedBy
    ) {
    }

    @RequestMapping(value = "/{id}/supervisor-confirmation", method = {org.springframework.web.bind.annotation.RequestMethod.PATCH, org.springframework.web.bind.annotation.RequestMethod.PUT})
    @Operation(summary = "Update Supervisor Confirmation status (NEW or SEEN) for an Out-of-Office Notification")
    public ResponseEntity<OutOfOfficeNotificationResponse> updateSupervisorConfirmation(
            @PathVariable UUID id,
            @RequestBody(required = false) SupervisorConfirmationRequest request) {
        SupervisorConfirmationStatus status = request != null && request.confirmationStatus() != null
                ? request.confirmationStatus()
                : SupervisorConfirmationStatus.SEEN;
        String confirmedBy = request != null ? request.confirmedBy() : null;
        return ResponseEntity.ok(outOfOfficeService.updateSupervisorConfirmation(id, status, confirmedBy));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an Out-of-Office Notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        outOfOfficeService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}

