package com.swizima.usersapi.dto;

/**
 * Result of assigning an account. {@code temporaryPassword} is only present when a new
 * system user was created with a generated password (shown once).
 */
public record AssignAccountResponse(
        ClientUserResponse clientUser,
        SystemUserResponse user,
        boolean userCreated,
        String temporaryPassword,
        /** Masked email the credentials were sent to, or null. */
        String credentialsSentTo,
        /** Why credentials weren't sent (e.g. no email), or null. */
        String deliveryNote
) {
}
