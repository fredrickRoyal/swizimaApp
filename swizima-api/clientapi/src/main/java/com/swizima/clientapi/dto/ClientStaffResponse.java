package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.ClientStaff;
import com.swizima.clientapi.entity.enums.Gender;
import com.swizima.clientapi.entity.enums.RecordStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Client staff member returned by the API.
 */
public record ClientStaffResponse(
        UUID id,
        UUID clientId,
        String clientCode,
        String clientName,
        String surname,
        String givenName,
        String fullName,
        Gender gender,
        String phoneNumber,
        String alternativePhone,
        String email,
        String position,
        String nin,
        String summary,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        RecordStatus recordStatus
) {

    public static ClientStaffResponse from(ClientStaff s) {
        return new ClientStaffResponse(
                s.getId(),
                s.getClient().getId(),
                s.getClient().getClientCode(),
                s.getClient().getClientName(),
                s.getSurname(),
                s.getGivenName(),
                (s.getGivenName() + " " + s.getSurname()).trim(),
                s.getGender(),
                s.getPhoneNumber(),
                s.getAlternativePhone(),
                s.getEmail(),
                s.getPosition(),
                s.getNin(),
                s.getSummary(),
                s.getCreatedBy(),
                s.getCreatedAt(),
                s.getUpdatedBy(),
                s.getUpdatedAt(),
                s.getRecordStatus()
        );
    }
}
