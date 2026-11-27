package com.swizima.stafftrackapi.dto;

import com.swizima.stafftrackapi.entity.enums.StaffStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for registering or updating a staff officer in the OPM Staff Directory.
 */
public record StaffMemberRequest(
        UUID clientRef,

        @NotBlank(message = "surname is required")
        @Size(max = 100)
        String surname,

        @NotBlank(message = "lastName is required")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "designation is required")
        @Size(max = 180)
        String designation,

        UUID designationId,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 180)
        String email,

        @Size(max = 180)
        String alternativeEmail,

        @NotBlank(message = "phone is required")
        @Size(max = 40)
        String phone,

        @Size(max = 40)
        String alternativePhone,

        @NotBlank(message = "department is required")
        @Size(max = 200)
        String department,

        UUID departmentId,

        @NotBlank(message = "dutyStation is required")
        @Size(max = 180)
        String dutyStation,

        UUID dutyStationId,

        StaffStatus status
) {
}

