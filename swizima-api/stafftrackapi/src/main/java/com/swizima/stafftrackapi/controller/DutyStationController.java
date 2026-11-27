package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.dto.MasterLookupRequest;
import com.swizima.stafftrackapi.dto.MasterLookupResponse;
import com.swizima.stafftrackapi.dto.PageResponse;
import com.swizima.stafftrackapi.service.DutyStationService;
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
@RequestMapping("/api/v1/stafftrack/duty-stations")
@RequiredArgsConstructor
@Tag(name = "Duty Stations", description = "OPM Offices, Regional Refugee Desks, and Refugee Settlements master registry")
public class DutyStationController {

    private final DutyStationService dutyStationService;

    @PostMapping
    @Operation(summary = "Register a new Duty Station / Office / Refugee Settlement")
    public ResponseEntity<MasterLookupResponse> createDutyStation(@Valid @RequestBody MasterLookupRequest request) {
        MasterLookupResponse created = dutyStationService.createDutyStation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "Search and paginate Duty Stations")
    public ResponseEntity<PageResponse<MasterLookupResponse>> searchDutyStations(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(dutyStationService.searchDutyStations(query, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "List all active Duty Stations")
    public ResponseEntity<List<MasterLookupResponse>> getAllDutyStations() {
        return ResponseEntity.ok(dutyStationService.getAllDutyStations());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a Duty Station by ID")
    public ResponseEntity<MasterLookupResponse> getDutyStation(@PathVariable UUID id) {
        return ResponseEntity.ok(dutyStationService.getDutyStation(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a Duty Station")
    public ResponseEntity<MasterLookupResponse> updateDutyStation(@PathVariable UUID id,
                                                                  @Valid @RequestBody MasterLookupRequest request) {
        return ResponseEntity.ok(dutyStationService.updateDutyStation(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a Duty Station")
    public ResponseEntity<Void> deleteDutyStation(@PathVariable UUID id) {
        dutyStationService.deleteDutyStation(id);
        return ResponseEntity.noContent().build();
    }
}

