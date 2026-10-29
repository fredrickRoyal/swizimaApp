package com.swizima.clientapi.dto;

import com.swizima.clientapi.entity.enums.ClientStatus;
import com.swizima.clientapi.entity.enums.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating a client.
 */
public record ClientRequest(
        @NotBlank(message = "Client code is required")
        @Size(max = 50)
        String clientCode,

        @NotBlank(message = "Client name is required")
        @Size(max = 255)
        String clientName,

        @Size(max = 255)
        String tradingName,

        ClientType clientType,

        @Size(max = 150)
        String industrySector,

        String description,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 30)
        String phoneNumber,

        @Size(max = 30)
        String alternativePhone,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String country,

        @Size(max = 100)
        String district,

        @Size(max = 100)
        String city,

        @Size(max = 150)
        String contactPerson,

        @Size(max = 100)
        String contactPersonTitle,

        @Email(message = "Contact person email must be valid")
        @Size(max = 150)
        String contactPersonEmail,

        @Size(max = 30)
        String contactPersonPhone,

        ClientStatus status
) {
}
