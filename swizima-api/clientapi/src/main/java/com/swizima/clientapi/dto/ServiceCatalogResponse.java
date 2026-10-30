package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.ServiceCatalog;
import com.swizima.clientapi.entity.enums.RecordStatus;
import com.swizima.clientapi.entity.enums.ServiceCatalogStatus;
import com.swizima.clientapi.entity.enums.ServiceCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service catalog item returned by the API.
 */
public record ServiceCatalogResponse(
        UUID id,
        UUID clientId,
        String clientCode,
        String clientName,
        String serviceCode,
        String serviceName,
        ServiceCategory category,
        String description,
        BigDecimal price,
        String currency,
        Integer durationMinutes,
        ServiceCatalogStatus status,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        RecordStatus recordStatus
) {

    public static ServiceCatalogResponse from(ServiceCatalog s) {
        return new ServiceCatalogResponse(
                s.getId(),
                s.getClient().getId(),
                s.getClient().getClientCode(),
                s.getClient().getClientName(),
                s.getServiceCode(),
                s.getServiceName(),
                s.getCategory(),
                s.getDescription(),
                s.getPrice(),
                s.getCurrency(),
                s.getDurationMinutes(),
                s.getStatus(),
                s.getCreatedBy(),
                s.getCreatedAt(),
                s.getUpdatedBy(),
                s.getUpdatedAt(),
                s.getRecordStatus()
        );
    }
}
