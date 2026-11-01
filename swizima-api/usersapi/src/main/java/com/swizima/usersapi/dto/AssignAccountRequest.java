package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * POST /api/v1/users/client-users/assign-account.
 * {@code username} is the sign-in id (email or phone). {@code phoneNumber}/{@code email} are extra contacts.
 */
public record AssignAccountRequest(
        @NotNull(message = "Client is required") UUID clientRef,
        UUID staffRef,
        @NotBlank(message = "Username is required") @Size(max = 150) String username,
        @Size(max = 30) String phoneNumber,
        @Size(max = 150) String email,
        OtpChannel preferredOtpChannel,
        Boolean isDefaultClient,
        /** Email the temporary password to the user (default true). */
        Boolean sendCredentials,
        /** Client name used in the welcome message. */
        @Size(max = 200) String organisationName
) {
}
