package com.swizima.usersapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for a user changing their own password (including the forced change after a
 * system-generated password).
 */
public record ChangePasswordRequest(
        @Schema(description = "Email address or phone number", example = "+256700123456")
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {

    @Schema(hidden = true)
    @AssertTrue(message = "New password and confirmation do not match")
    public boolean isPasswordConfirmed() {
        return newPassword == null || newPassword.equals(confirmPassword);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "New password must be different from the current password")
    public boolean isPasswordChanged() {
        return newPassword == null || !newPassword.equals(currentPassword);
    }
}
