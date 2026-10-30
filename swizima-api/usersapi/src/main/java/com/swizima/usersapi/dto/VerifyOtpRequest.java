package com.swizima.usersapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotNull(message = "OTP reference is required")
        UUID otpReference,

        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "^\\d{4,8}$", message = "OTP code must be numeric")
        String code
) {
}
