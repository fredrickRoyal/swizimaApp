package com.swizima.configurationsapi.controller;

import com.swizima.configurationsapi.dto.BillingPlanRequest;
import com.swizima.configurationsapi.dto.BillingPlanResponse;
import com.swizima.configurationsapi.dto.BillingPlanStatusRequest;
import com.swizima.configurationsapi.dto.PageResponse;
import com.swizima.configurationsapi.entity.enums.BillingPlanStatus;
import com.swizima.configurationsapi.service.BillingPlanService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configurations/billing/plans")
@RequiredArgsConstructor
@Tag(name = "Billing Plans", description = "Per-client percentage charged on every transaction")
public class BillingPlanController {

    private final BillingPlanService planService;

    @PostMapping
    @Operation(summary = "Map a client to a per-transaction percentage")
    public ResponseEntity<BillingPlanResponse> createPlan(@Valid @RequestBody BillingPlanRequest request) {
        BillingPlanResponse created = planService.createPlan(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<BillingPlanResponse>> getPlans(
            @RequestParam(required = false) BillingPlanStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(planService.getPlans(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingPlanResponse> getPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @GetMapping("/client/{clientRef}")
    @Operation(summary = "Get a client's billing plan (cached; used by the Payments API on every charge)")
    public ResponseEntity<BillingPlanResponse> getPlanByClient(@PathVariable UUID clientRef) {
        return ResponseEntity.ok(planService.getPlanByClient(clientRef));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update the percentage / settings (applies to future charges only)")
    public ResponseEntity<BillingPlanResponse> updatePlan(@PathVariable UUID id,
                                                          @Valid @RequestBody BillingPlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or suspend billing (suspended clients cannot create transactions)")
    public ResponseEntity<BillingPlanResponse> updatePlanStatus(@PathVariable UUID id,
                                                                @Valid @RequestBody BillingPlanStatusRequest request) {
        return ResponseEntity.ok(planService.updatePlanStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
