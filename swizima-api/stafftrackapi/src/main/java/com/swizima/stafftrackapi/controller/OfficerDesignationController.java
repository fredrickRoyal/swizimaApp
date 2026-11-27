package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.service.OfficerDesignationService;
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
@RequestMapping("/api/v1/stafftrack/designations")
@RequiredArgsConstructor
@Tag(name = "Officer Designations", description = "OPM Officer Designations / Job Titles master registry")
public class OfficerDesignationController {

    private final OfficerDesignationService designationService;

    @PostMapping
    @Operation(summary = "Register a new Officer Designation")
    public ResponseEntity<MasterLookupResponse> createDesignation(@Valid @RequestBody MasterLookupRequest request) {
        MasterLookupResponse created = designationService.createDesignation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search and paginate Officer Designations")
    public ResponseEntity<PageResponse<MasterLookupResponse>> searchDesignations(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(designationService.searchDesignations(query, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "List all active Officer Designations")
    public ResponseEntity<List<MasterLookupResponse>> getAllDesignations() {
        return ResponseEntity.ok(designationService.getAllDesignations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an Officer Designation by ID")
    public ResponseEntity<MasterLookupResponse> getDesignation(@PathVariable UUID id) {
        return ResponseEntity.ok(designationService.getDesignation(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an Officer Designation")
    public ResponseEntity<MasterLookupResponse> updateDesignation(@PathVariable UUID id,
                                                                  @Valid @RequestBody MasterLookupRequest request) {
        return ResponseEntity.ok(designationService.updateDesignation(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an Officer Designation")
    public ResponseEntity<Void> deleteDesignation(@PathVariable UUID id) {
        designationService.deleteDesignation(id);
        return ResponseEntity.noContent().build();
    }
}

