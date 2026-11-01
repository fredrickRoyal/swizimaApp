package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Passwordless sign-in: the user only supplies their email or phone number. */
public record OtpLoginRequest(
        @Schema(description = "Email address or phone number (any common format)", example = "0700123456")
        @NotBlank(message = "Username is required")
        @Size(max = 150)
        String username,

        @Schema(description = "Where to receive the OTP. Optional - defaults to the user's preferred channel")
        OtpChannel otpChannel
) {
}
