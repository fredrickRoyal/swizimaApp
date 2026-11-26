package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.entity.ClientUserAssignment;
import com.swizima.stafftrackapi.entity.Department;
import com.swizima.stafftrackapi.entity.UserAccount;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.ClientUserAssignmentRepository;
import com.swizima.stafftrackapi.repository.DepartmentRepository;
import com.swizima.stafftrackapi.repository.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Assign and manage System Users and Client/Department User Links")
public class UserManagementController {

    private final UserAccountRepository userAccountRepository;
    private final ClientUserAssignmentRepository clientUserRepository;
    private final DepartmentRepository departmentRepository;

    public record SystemUserDto(
            UUID id,
            UUID staffId,
            String fullName,
            String username,
            String email,
            String phone,
            String designation,
            String department,
            String dutyStation,
            String role,
            String preferredOtpChannel,
            String status,
            String lastLoginAt,
            String createdAt
    ) {
        public static SystemUserDto from(UserAccount u) {
            return new SystemUserDto(
                    u.getId(),
                    u.getStaffId(),
                    u.getFullName(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getPhone(),
                    u.getDesignation(),
                    u.getDepartment(),
                    u.getDutyStation(),
                    u.getRole(),
                    u.getPreferredOtpChannel(),
                    u.getStatus(),
                    u.getLastLoginAt(),
                    u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString()
            );
        }
    }

    public record SystemUserRequest(
            UUID staffId,
            @NotBlank String fullName,
            String username,
            @NotBlank String email,
            @NotBlank String phone,
            @NotBlank String designation,
            @NotBlank String department,
            @NotBlank String dutyStation,
            @NotBlank String role,
            @NotBlank String preferredOtpChannel,
            String status
    ) {
    }

    public record ClientUserDto(
            UUID id,
            UUID systemUserId,
            String userPhoneNumber,
            String userEmail,
            String accountCategory,
            String clientRef,
            String clientName,
            String clientCode,
            boolean isDefaultClient,
            String activationStatus,
            String createdAt
    ) {
        public static ClientUserDto from(ClientUserAssignment cu) {
            return new ClientUserDto(
                    cu.getId(),
                    cu.getSystemUserId(),
                    cu.getUserPhoneNumber(),
                    cu.getUserEmail(),
                    cu.getAccountCategory(),
                    cu.getClientRef() != null ? cu.getClientRef().toString() : cu.getClientCode(),
                    cu.getClientName(),
                    cu.getClientCode(),
                    cu.isDefaultClient(),
                    cu.getActivationStatus(),
                    cu.getCreatedAt() != null ? cu.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString()
            );
        }
    }

    public record ClientUserRequest(
            UUID systemUserId,
            String userPhoneNumber,
            String userEmail,
            @NotBlank String accountCategory,
            UUID clientRef,
            @NotBlank String clientName,
            @NotBlank String clientCode,
            Boolean isDefaultClient,
            String activationStatus
    ) {
    }

    @GetMapping("/users/all")
    @Operation(summary = "List all assigned System Users")
    public ResponseEntity<List<SystemUserDto>> getAllUsers() {
        List<SystemUserDto> list = userAccountRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .map(SystemUserDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/users")
    @Transactional
    @Operation(summary = "Assign a Staff Member or create a System User")
    public ResponseEntity<SystemUserDto> createUser(@RequestBody SystemUserRequest request) {
        UserAccount entity = UserAccount.builder()
                .staffId(request.staffId())
                .fullName(request.fullName().trim())
                .username((request.username() != null && !request.username().isBlank()
                        ? request.username()
                        : request.email()).trim().toLowerCase())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone().trim())
                .designation(request.designation().trim())
                .department(request.department().trim())
                .dutyStation(request.dutyStation().trim())
                .role(request.role().trim())
                .preferredOtpChannel(request.preferredOtpChannel().trim())
                .status(request.status() != null ? request.status() : "ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();

        UserAccount saved = userAccountRepository.save(entity);
        ensureClientUserAssignmentForSystemUser(saved);
        return ResponseEntity.ok(SystemUserDto.from(saved));
    }

    @PutMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Update an assigned System User")
    public ResponseEntity<SystemUserDto> updateUser(@PathVariable UUID id, @RequestBody SystemUserRequest request) {
        UserAccount entity = userAccountRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User account with id '" + id + "' not found"));

        entity.setStaffId(request.staffId());
        entity.setFullName(request.fullName().trim());
        entity.setUsername((request.username() != null && !request.username().isBlank()
                ? request.username()
                : request.email()).trim().toLowerCase());
        entity.setEmail(request.email().trim().toLowerCase());
        entity.setPhone(request.phone().trim());
        entity.setDesignation(request.designation().trim());
        entity.setDepartment(request.department().trim());
        entity.setDutyStation(request.dutyStation().trim());
        entity.setRole(request.role().trim());
        entity.setPreferredOtpChannel(request.preferredOtpChannel().trim());
        if (request.status() != null) {
            entity.setStatus(request.status());
        }

        UserAccount saved = userAccountRepository.save(entity);
        return ResponseEntity.ok(SystemUserDto.from(saved));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    @Operation(summary = "Update System User status (ACTIVE, SUSPENDED, PENDING_ACTIVATION)")
    public ResponseEntity<SystemUserDto> updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UserAccount entity = userAccountRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User account with id '" + id + "' not found"));
        String status = body.getOrDefault("status", "ACTIVE");
        entity.setStatus(status);
        return ResponseEntity.ok(SystemUserDto.from(userAccountRepository.save(entity)));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Remove a System User assignment")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        UserAccount entity = userAccountRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User account with id '" + id + "' not found"));
        entity.setRecordStatus(RecordStatus.DELETED);
        userAccountRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    // ---------- Client Users ----------
    @GetMapping("/client-users/all")
    @Operation(summary = "List all Client / Department user assignments")
    public ResponseEntity<List<ClientUserDto>> getAllClientUsers() {
        List<ClientUserDto> list = clientUserRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .map(ClientUserDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/client-users")
    @Transactional
    @Operation(summary = "Assign a user to a Client / Department")
    public ResponseEntity<ClientUserDto> createClientUser(@RequestBody ClientUserRequest request) {
        boolean makeDefault = Boolean.TRUE.equals(request.isDefaultClient());
        if (makeDefault) {
            clearDefaultForPerson(request.userEmail(), request.userPhoneNumber());
        }

        ClientUserAssignment cu = ClientUserAssignment.builder()
                .clientRef(request.clientRef())
                .systemUserId(request.systemUserId())
                .userPhoneNumber(request.userPhoneNumber())
                .userEmail(request.userEmail() != null ? request.userEmail().trim().toLowerCase() : null)
                .accountCategory(request.accountCategory())
                .clientName(request.clientName().trim())
                .clientCode(request.clientCode().trim())
                .defaultClient(makeDefault)
                .activationStatus(request.activationStatus() != null ? request.activationStatus() : "ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();

        return ResponseEntity.ok(ClientUserDto.from(clientUserRepository.save(cu)));
    }

    @PatchMapping("/client-users/{id}/status")
    @Transactional
    @Operation(summary = "Update Client User activation status")
    public ResponseEntity<ClientUserDto> updateClientUserStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ClientUserAssignment cu = clientUserRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client user assignment with id '" + id + "' not found"));
        cu.setActivationStatus(body.getOrDefault("activationStatus", "ACTIVE"));
        return ResponseEntity.ok(ClientUserDto.from(clientUserRepository.save(cu)));
    }

    @PatchMapping("/client-users/{id}/default")
    @Transactional
    @Operation(summary = "Make this Client / Department the user's default organisation")
    public ResponseEntity<ClientUserDto> makeDefaultClientUser(@PathVariable UUID id) {
        ClientUserAssignment target = clientUserRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client user assignment with id '" + id + "' not found"));
        clearDefaultForPerson(target.getUserEmail(), target.getUserPhoneNumber());
        target.setDefaultClient(true);
        return ResponseEntity.ok(ClientUserDto.from(clientUserRepository.save(target)));
    }

    @DeleteMapping("/client-users/{id}")
    @Transactional
    @Operation(summary = "Remove a Client User assignment")
    public ResponseEntity<Void> deleteClientUser(@PathVariable UUID id) {
        ClientUserAssignment cu = clientUserRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Client user assignment with id '" + id + "' not found"));
        cu.setRecordStatus(RecordStatus.DELETED);
        clientUserRepository.save(cu);
        return ResponseEntity.noContent().build();
    }

    private void ensureClientUserAssignmentForSystemUser(UserAccount saved) {
        Optional<Department> deptOpt = departmentRepository.findByNameIgnoreCaseAndRecordStatusNot(
                saved.getDepartment(), RecordStatus.DELETED);
        String clientCode = deptOpt.map(Department::getCode).orElse("OPM-DEPT");
        UUID clientRef = deptOpt.map(Department::getId).orElse(null);

        List<ClientUserAssignment> existing = clientUserRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(cu -> saved.getEmail().equalsIgnoreCase(cu.getUserEmail()))
                .toList();

        boolean alreadyLinked = existing.stream()
                .anyMatch(cu -> saved.getDepartment().equalsIgnoreCase(cu.getClientName()));
        if (alreadyLinked) {
            return;
        }

        String category = switch (saved.getRole()) {
            case "SUPER_ADMIN" -> "SUPER_ADMIN";
            case "DEPARTMENT_HEAD", "DESK_OFFICER" -> "CLIENT_ADMIN";
            default -> "USER";
        };

        ClientUserAssignment link = ClientUserAssignment.builder()
                .clientRef(clientRef)
                .systemUserId(saved.getId())
                .userPhoneNumber(saved.getPhone())
                .userEmail(saved.getEmail())
                .accountCategory(category)
                .clientName(saved.getDepartment())
                .clientCode(clientCode)
                .defaultClient(existing.isEmpty())
                .activationStatus(saved.getStatus())
                .recordStatus(RecordStatus.ACTIVE)
                .build();
        clientUserRepository.save(link);
    }

    private void clearDefaultForPerson(String email, String phone) {
        List<ClientUserAssignment> all = clientUserRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED);
        for (ClientUserAssignment cu : all) {
            boolean sameEmail = email != null && !email.isBlank() && email.equalsIgnoreCase(cu.getUserEmail());
            boolean samePhone = phone != null && !phone.isBlank() && phone.equals(cu.getUserPhoneNumber());
            if ((sameEmail || samePhone) && cu.isDefaultClient()) {
                cu.setDefaultClient(false);
                clientUserRepository.save(cu);
            }
        }
    }
}

