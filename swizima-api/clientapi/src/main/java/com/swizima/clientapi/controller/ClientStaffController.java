package com.swizima.clientapi.controller;

import com.swizima.clientapi.dto.ClientStaffRequest;
import com.swizima.clientapi.dto.ClientStaffResponse;
import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.entity.enums.Gender;
import com.swizima.clientapi.service.ClientStaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Staff are a sub-resource of a client: /api/v1/clients/{clientId}/staff.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/staff")
@RequiredArgsConstructor
@Tag(name = "Client Staff", description = "People working for a client")
public class ClientStaffController {

    private final ClientStaffService staffService;

    @PostMapping
    public ResponseEntity<ClientStaffResponse> createStaff(@PathVariable UUID clientId,
                                                           @Valid @RequestBody ClientStaffRequest request) {
        ClientStaffResponse created = staffService.createStaff(clientId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ClientStaffResponse>> getStaff(
            @PathVariable UUID clientId,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "surname", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(staffService.getStaff(clientId, gender, search, pageable));
    }

    @GetMapping("/{staffId}")
    public ResponseEntity<ClientStaffResponse> getStaffById(@PathVariable UUID clientId, @PathVariable UUID staffId) {
        return ResponseEntity.ok(staffService.getStaffById(clientId, staffId));
    }

    @PutMapping("/{staffId}")
    public ResponseEntity<ClientStaffResponse> updateStaff(@PathVariable UUID clientId,
                                                           @PathVariable UUID staffId,
                                                           @Valid @RequestBody ClientStaffRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(clientId, staffId, request));
    }

    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID clientId, @PathVariable UUID staffId) {
        staffService.deleteStaff(clientId, staffId);
        return ResponseEntity.noContent().build();
    }
}
