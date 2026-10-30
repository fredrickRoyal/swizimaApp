package com.swizima.clientapi.controller;

import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.dto.ServiceCatalogRequest;
import com.swizima.clientapi.dto.ServiceCatalogResponse;
import com.swizima.clientapi.dto.ServiceCatalogStatusRequest;
import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import com.swizima.clientapi.service.ServiceCatalogService;
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
@RequestMapping("/api/v1/service-catalogs")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Services offered by clients")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> createService(@Valid @RequestBody ServiceCatalogRequest request) {
        ServiceCatalogResponse created = serviceCatalogService.createService(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ServiceCatalogResponse>> getServices(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) ServiceCatalogStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "serviceName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(serviceCatalogService.getServices(clientId, category, status, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> getServiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceCatalogService.getServiceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> updateService(@PathVariable UUID id,
                                                                @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceCatalogResponse> updateServiceStatus(@PathVariable UUID id,
                                                                      @Valid @RequestBody ServiceCatalogStatusRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateServiceStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        serviceCatalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
