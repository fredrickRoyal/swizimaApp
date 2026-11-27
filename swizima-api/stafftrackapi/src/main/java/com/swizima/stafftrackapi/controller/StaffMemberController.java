package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.dto.StaffMemberRequest;
import com.swizima.stafftrackapi.dto.StaffMemberResponse;
import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import com.swizima.stafftrackapi.service.StaffMemberService;
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
@RequestMapping("/api/v1/stafftrack/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Register", description = "Register and manage OPM staff officers in the Staff Directory")
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    @PostMapping
    @Operation(summary = "Register a new staff officer")
    public ResponseEntity<StaffMemberResponse> registerStaff(@Valid @RequestBody StaffMemberRequest request) {
        StaffMemberResponse created = staffMemberService.registerStaff(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search and paginate registered staff officers")
    public ResponseEntity<PageResponse<StaffMemberResponse>> searchStaff(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String dutyStation,
            @RequestParam(required = false) StaffStatus status,
            @PageableDefault(size = 50, sort = "surname", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(staffMemberService.searchStaff(query, department, dutyStation, status, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "List all registered staff officers")
    public ResponseEntity<List<StaffMemberResponse>> getAllStaff() {
        return ResponseEntity.ok(staffMemberService.getAllStaff());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a registered staff officer by ID")
    public ResponseEntity<StaffMemberResponse> getStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(staffMemberService.getStaff(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a registered staff officer")
    public ResponseEntity<StaffMemberResponse> updateStaff(@PathVariable UUID id,
                                                           @Valid @RequestBody StaffMemberRequest request) {
        return ResponseEntity.ok(staffMemberService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a registered staff officer")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID id) {
        staffMemberService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }
}

