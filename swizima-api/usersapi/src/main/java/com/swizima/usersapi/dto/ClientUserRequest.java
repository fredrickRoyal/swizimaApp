package com.swizima.usersapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swizima.usersapi.entity.enums.ActivationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Create/update body for a client user. At least one of phone number or email is required.
 * {@code activationStatus} defaults to PENDING_ACTIVATION; {@code isDefaultClient} to false.
 */
public record ClientUserRequest(
        @NotNull(message = "Client is required") UUID clientRef,
        UUID staffRef,
        @Size(max = 30) String userPhoneNumber,
        @Email(message = "Invalid email address") @Size(max = 150) String userEmail,
        ActivationStatus activationStatus,
        Boolean isDefaultClient
) {
    @JsonIgnore
    @AssertTrue(message = "Provide a phone number or an email address")
    public boolean isContactProvided() {
        return (userPhoneNumber != null && !userPhoneNumber.isBlank())
                || (userEmail != null && !userEmail.isBlank());
    }
}
