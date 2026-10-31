package com.swizima.usersapi.dto;

import com.swizima.usersapi.entity.enums.OtpChannel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Returned after a successful password check: an OTP has been sent and must be verified to finish login.
 */
public record OtpChallengeResponse(
        @Schema(description = "Send this back with the code to /api/v1/auth/verify-otp or /resend-otp")
        UUID otpReference,
        OtpChannel channel,
        @Schema(example = "ja******@mail.com")
        String maskedDestination,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt,
        @Schema(description = "Seconds until the code expires (timezone-independent, use for countdowns)", example = "300")
        long expiresInSeconds,
        @Schema(description = "Seconds until a new code can be requested", example = "60")
        long resendAvailableInSeconds,
        String message
) {
}
