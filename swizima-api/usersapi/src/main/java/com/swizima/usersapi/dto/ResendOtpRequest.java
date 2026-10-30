package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendOtpRequest(
        @NotNull(message = "OTP reference is required")
        UUID otpReference,

        @Schema(description = "Optional. Switch to a different channel; defaults to the original channel")
        OtpChannel channel
) {
}
