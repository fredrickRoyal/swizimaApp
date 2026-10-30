package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.ActivationStatus;
import com.swizima.usersapi.entity.enums.UsernameType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        UsernameType usernameType,
        ActivationStatus activationStatus,

        @Schema(description = "When true, the client must redirect the user to change their password before continuing")
        boolean mustChangePassword,

        LocalDateTime lastLoginAt,
        String message
) {
}
