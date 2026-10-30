package com.swizima.usersapi.dto;

/**
 * Shared password policy used by request DTOs.
 */
public final class PasswordPolicy {

    /** At least 8 characters, containing at least one letter and one digit. */
    public static final String REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$";

    public static final String MESSAGE =
            "Password must be 8-100 characters and contain at least one letter and one digit";

    private PasswordPolicy() {
    }
}
