package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Email address or phone number (any common format)", example = "0700123456")
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password,

        @Schema(description = "Where to receive the OTP. Optional - defaults to the user's preferred channel (EMAIL by default)")
        OtpChannel otpChannel
) {
}
