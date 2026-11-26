package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.entity.Organisation;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.DuplicateResourceException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.OrganisationRepository;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stafftrack/organisations")
@RequiredArgsConstructor
@Tag(name = "Organisations", description = "Register and manage client organisations (e.g. OPM)")
public class OrganisationController {

    private final OrganisationRepository organisationRepository;

    public record OrganisationDto(
            UUID id,
            String code,
            String name,
            String organisationType,
            String contactPerson,
            String email,
            String phone,
            String city,
            String country,
            String address,
            String status,
            String createdAt
    ) {
        public static OrganisationDto from(Organisation o) {
            return new OrganisationDto(
                    o.getId(),
                    o.getCode(),
                    o.getName(),
                    o.getOrganisationType(),
                    o.getContactPerson(),
                    o.getEmail(),
                    o.getPhone(),
                    o.getCity(),
                    o.getCountry(),
                    o.getAddress(),
                    o.getStatus(),
                    o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString()
            );
        }
    }

    public record OrganisationRequest(
            @NotBlank String code,
            @NotBlank String name,
            String organisationType,
            String contactPerson,
            String email,
            String phone,
            String city,
            String country,
            String address,
            String status
    ) {
    }

    @GetMapping("/all")
    @Operation(summary = "List all active Organisations")
    public ResponseEntity<List<OrganisationDto>> getAllOrganisations() {
        List<OrganisationDto> list = organisationRepository
                .findAllByRecordStatusNotOrderByCreatedAtAsc(RecordStatus.DELETED)
                .stream()
                .map(OrganisationDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Register a new Organisation")
    public ResponseEntity<OrganisationDto> createOrganisation(@RequestBody OrganisationRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        organisationRepository.findByCodeIgnoreCaseAndRecordStatusNot(cleanCode, RecordStatus.DELETED)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Organisation with code '" + cleanCode + "' already exists");
                });

        Organisation entity = Organisation.builder()
                .code(cleanCode)
                .name(request.name().trim())
                .organisationType(request.organisationType() != null && !request.organisationType().isBlank()
                        ? request.organisationType().trim()
                        : "Government Ministry / Agency")
                .contactPerson(request.contactPerson() != null ? request.contactPerson().trim() : "")
                .email(request.email() != null ? request.email().trim().toLowerCase() : "")
                .phone(request.phone() != null ? request.phone().trim() : "")
                .city(request.city() != null ? request.city().trim() : "Kampala")
                .country(request.country() != null ? request.country().trim() : "Uganda")
                .address(request.address() != null ? request.address().trim() : "")
                .status(request.status() != null ? request.status() : "ACTIVE")
                .recordStatus(RecordStatus.ACTIVE)
                .build();

        return ResponseEntity.ok(OrganisationDto.from(organisationRepository.save(entity)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update an Organisation")
    public ResponseEntity<OrganisationDto> updateOrganisation(@PathVariable UUID id,
                                                              @RequestBody OrganisationRequest request) {
        Organisation entity = organisationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation with id '" + id + "' not found"));

        entity.setCode(request.code().trim().toUpperCase());
        entity.setName(request.name().trim());
        if (request.organisationType() != null) entity.setOrganisationType(request.organisationType().trim());
        if (request.contactPerson() != null) entity.setContactPerson(request.contactPerson().trim());
        if (request.email() != null) entity.setEmail(request.email().trim().toLowerCase());
        if (request.phone() != null) entity.setPhone(request.phone().trim());
        if (request.city() != null) entity.setCity(request.city().trim());
        if (request.country() != null) entity.setCountry(request.country().trim());
        if (request.address() != null) entity.setAddress(request.address().trim());
        if (request.status() != null) entity.setStatus(request.status());

        return ResponseEntity.ok(OrganisationDto.from(organisationRepository.save(entity)));
    }

    @PatchMapping("/{id}/status")
    @Transactional
    @Operation(summary = "Update Organisation status (ACTIVE, SUSPENDED, INACTIVE, PENDING)")
    public ResponseEntity<OrganisationDto> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Organisation entity = organisationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation with id '" + id + "' not found"));
        entity.setStatus(body.getOrDefault("status", "ACTIVE"));
        return ResponseEntity.ok(OrganisationDto.from(organisationRepository.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Soft-delete an Organisation")
    public ResponseEntity<Void> deleteOrganisation(@PathVariable UUID id) {
        Organisation entity = organisationRepository.findByIdAndRecordStatusNot(id, RecordStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation with id '" + id + "' not found"));
        entity.setRecordStatus(RecordStatus.DELETED);
        organisationRepository.save(entity);
        return ResponseEntity.noContent().build();
    }
}

