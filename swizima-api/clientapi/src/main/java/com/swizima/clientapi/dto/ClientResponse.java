package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.Client;
import com.swizima.clientapi.entity.enums.ClientStatus;
import com.swizima.clientapi.entity.enums.ClientType;
import com.swizima.clientapi.entity.enums.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client representation returned by the API.
 */
public record ClientResponse(
        UUID id,
        String clientCode,
        String clientName,
        String tradingName,
        ClientType clientType,
        String industrySector,
        String description,
        String email,
        String phoneNumber,
        String alternativePhone,
        String address,
        String country,
        String district,
        String city,
        String contactPerson,
        String contactPersonTitle,
        String contactPersonEmail,
        String contactPersonPhone,
        ClientStatus status,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        RecordStatus recordStatus
) {

    public static ClientResponse from(Client c) {
        return new ClientResponse(
                c.getId(),
                c.getClientCode(),
                c.getClientName(),
                c.getTradingName(),
                c.getClientType(),
                c.getIndustrySector(),
                c.getDescription(),
                c.getEmail(),
                c.getPhoneNumber(),
                c.getAlternativePhone(),
                c.getAddress(),
                c.getCountry(),
                c.getDistrict(),
                c.getCity(),
                c.getContactPerson(),
                c.getContactPersonTitle(),
                c.getContactPersonEmail(),
                c.getContactPersonPhone(),
                c.getStatus(),
                c.getCreatedBy(),
                c.getCreatedAt(),
                c.getUpdatedBy(),
                c.getUpdatedAt(),
                c.getRecordStatus()
        );
    }
}
