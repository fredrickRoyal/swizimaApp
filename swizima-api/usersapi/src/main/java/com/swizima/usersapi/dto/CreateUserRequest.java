package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a user.
 * If {@code password} is omitted, a temporary password is generated and the user must change it on first login.
 */
public record CreateUserRequest(
        @Schema(description = "Email address or phone number", example = "+256700123456")
        @NotBlank(message = "Username is required")
        @Size(max = 150)
        String username,

        @Schema(description = "Optional. Leave empty to have the system generate a temporary password")
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String password,

        @Schema(description = "Defaults to true", example = "true")
        Boolean enabled,

        @Schema(description = "Contact email for OTPs. Defaults to the username if the username is an email",
                example = "jane@example.com")
        @Size(max = 150)
        String email,

        @Schema(description = "Contact phone for SMS/WhatsApp OTPs. Defaults to the username if the username is a phone",
                example = "0700123456")
        @Size(max = 30)
        String phoneNumber,

        @Schema(description = "Default OTP channel. Defaults to EMAIL")
        OtpChannel preferredOtpChannel
) {
}
