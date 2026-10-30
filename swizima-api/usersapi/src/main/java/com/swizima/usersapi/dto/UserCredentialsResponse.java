package com.swizima.usersapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Returned when a user is created or their password is reset.
 * {@code temporaryPassword} is only present when the system generated it, and is shown once.
 */
public record UserCredentialsResponse(
        SystemUserResponse user,

        @Schema(description = "Only returned when the password was system-generated. "
                + "Deliver it to the user via a secure channel (SMS/email); it is not retrievable later.")
        String temporaryPassword
) {
}
