package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.BusinessRuleException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.SupervisorRepository;
import com.swizima.stafftrackapi.service.SupervisorNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({
        "/api/v1/stafftrack/supervisors",
        "/api/v1/stafftrack/notification-supervisors"
})
@RequiredArgsConstructor
@Tag(name = "Supervisors", description = "Manage entity supervisors list to receive notifications (at least email or phone required)")
public class SupervisorController {

    private final SupervisorRepository supervisorRepository;
    private final SupervisorNotificationService supervisorNotificationService;

    public static String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return "OVERALL_HEAD";
        }
        String norm = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        if ("UNIT_HEAD".equals(norm) || "SUPERVISOR".equals(norm) || "UNIT_SUPERVISOR".equals(norm)) {
            return "UNIT_HEAD";
        }
        return "OVERALL_HEAD";
    }

    public record SupervisorDto(
            UUID id,
            UUID clientRef,
            String entityName,
            String entityCode,
            String name,
            String email,
            String phone,
            String category,
            String department,
            String status,
            String createdAt
    ) {
        public static SupervisorDto from(Supervisor s) {
            return new SupervisorDto(
                    s.getId(),
                    s.getClientRef(),
                    s.getEntityName(),
                    s.getEntityCode(),
                    s.getName(),
                    s.getEmail(),
                    s.getPhone(),
                    normalizeCategory(s.getCategory()),
                    s.getDepartment(),
                    s.getStatus(),
                    s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString()
            );
        }
    }

    public record SupervisorRequest(
            UUID clientRef,
            String entityName,
            String entityCode,
            String name,
            String email,
            String phone,
            String category,
            String department,
            String status
    ) {
    }

    @GetMapping("/all")
    @Operation(summary = "List all active entity supervisors")
    public ResponseEntity<List<SupervisorDto>> getAllSupervisors() {
        List<SupervisorDto> list = supervisorRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .map(SupervisorDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Add an entity supervisor to receive notifications (either email or phone is required; other fields optional)")
    public ResponseEntity<SupervisorDto> createSupervisor(@RequestBody SupervisorRequest request) {
        String cleanEmail = request.email() != null ? request.email().trim().toLowerCase() : "";
        String cleanPhone = request.phone() != null ? request.phone().trim() : "";
        String cleanName = request.name() != null ? request.name().trim() : "";
        String cleanEntityName = request.entityName() != null ? request.entityName().trim() : "";
        String cleanEntityCode = request.entityCode() != null ? request.entityCode().trim().toUpperCase() : "";

        if (cleanEmail.isEmpty() && cleanPhone.isEmpty()) {
            throw new BusinessRuleException("At least an email address or a phone number is required.");
        }

        String cleanDepartment = request.department() != null ? request.department().trim() : "";

        Supervisor entity = Supervisor.builder()
                .clientRef(request.clientRef())
                .entityName(cleanEntityName)
                .entityCode(cleanEntityCode)
                .name(cleanName)
                .email(cleanEmail)
                .phone(cleanPhone)
                .category(normalizeCategory(request.category()))
                .department(cleanDepartment)
                .status(request.status() != null && !request.status().isBlank() ? request.status().trim() : "ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();

        return ResponseEntity.ok(SupervisorDto.from(supervisorRepository.save(entity)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update an entity supervisor")
    public ResponseEntity<SupervisorDto> updateSupervisor(@PathVariable UUID id,
                                                          @RequestBody SupervisorRequest request) {
        Supervisor entity = supervisorRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor with id '" + id + "' not found"));

        String cleanEmail = request.email() != null ? request.email().trim().toLowerCase() : "";
        String cleanPhone = request.phone() != null ? request.phone().trim() : "";
        String cleanName = request.name() != null ? request.name().trim() : "";

        if (cleanEmail.isEmpty() && cleanPhone.isEmpty()) {
            throw new BusinessRuleException("At least an email address or a phone number is required.");
        }

        if (request.clientRef() != null) {
            entity.setClientRef(request.clientRef());
        }
        if (request.entityName() != null) {
            entity.setEntityName(request.entityName().trim());
        }
        if (request.entityCode() != null) {
            entity.setEntityCode(request.entityCode().trim().toUpperCase());
        }
        entity.setName(cleanName);
        entity.setEmail(cleanEmail);
        entity.setPhone(cleanPhone);
        if (request.category() != null && !request.category().isBlank()) {
            entity.setCategory(normalizeCategory(request.category()));
        } else if (entity.getCategory() == null || entity.getCategory().isBlank()) {
            entity.setCategory("OVERALL_HEAD");
        }
        if (request.department() != null) {
            entity.setDepartment(request.department().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim());
        }

        return ResponseEntity.ok(SupervisorDto.from(supervisorRepository.save(entity)));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Transactional
    @Operation(summary = "Toggle supervisor status (ACTIVE / INACTIVE)")
    public ResponseEntity<SupervisorDto> updateStatus(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body) {
        Supervisor entity = supervisorRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor with id '" + id + "' not found"));
        entity.setStatus(body.getOrDefault("status", "ACTIVE"));
        return ResponseEntity.ok(SupervisorDto.from(supervisorRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Remove a supervisor")
    public ResponseEntity<Void> deleteSupervisor(@PathVariable UUID id) {
        Supervisor entity = supervisorRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Supervisor with id '" + id + "' not found"));
        entity.setRecordStatus(RecordStatus.DELETED);
        supervisorRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notify-whatsapp")
    @Operation(summary = "Send a WhatsApp notification to all active supervisors' configured phone numbers")
    public ResponseEntity<Map<String, Object>> notifySupervisorsWhatsApp(
            @RequestBody(required = false) Map<String, String> body) {
        String customMessage = body != null ? body.get("message") : null;
        return ResponseEntity.ok(supervisorNotificationService.notifyActiveSupervisorsViaWhatsApp(customMessage));
    }
}

