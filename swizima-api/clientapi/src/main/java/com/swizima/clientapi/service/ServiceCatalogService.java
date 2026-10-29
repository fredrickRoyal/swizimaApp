package com.swizima.clientapi.service;

import com.swizima.clientapi.dto.PageResponse;
import com.swizima.clientapi.dto.ServiceCatalogRequest;
import com.swizima.clientapi.dto.ServiceCatalogResponse;
import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceCatalogService {

    ServiceCatalogResponse createService(ServiceCatalogRequest request);

    ServiceCatalogResponse updateService(UUID id, ServiceCatalogRequest request);

    ServiceCatalogResponse getServiceById(UUID id);

    PageResponse<ServiceCatalogResponse> getServices(UUID clientId,
                                                     ServiceCategory category,
                                                     ServiceCatalogStatus status,
                                                     String search,
                                                     Pageable pageable);

    ServiceCatalogResponse updateServiceStatus(UUID id, ServiceCatalogStatus status);

    void deleteService(UUID id);
}
