package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating a user's username, contact details, OTP preference and enabled flag.
 */
public record UpdateUserRequest(
        @Schema(description = "Email address or phone number", example = "jane@example.com")
        @NotBlank(message = "Username is required")
        @Size(max = 150)
        String username,

        Boolean enabled,

        @Size(max = 150)
        String email,

        @Size(max = 30)
        String phoneNumber,

        OtpChannel preferredOtpChannel
) {
}
