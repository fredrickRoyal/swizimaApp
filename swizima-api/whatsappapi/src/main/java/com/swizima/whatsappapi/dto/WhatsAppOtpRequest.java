package com.swizima.whatsappapi.dto;

import jakarta.validation.constraints.NotBlank;

public record WhatsAppOtpRequest(
        @NotBlank(message = "Recipient phone number (to) is required")
        String to,
        @NotBlank(message = "OTP code is required")
        String code,
        Integer expiryMinutes,
        String appName,
        String templateName,
        String languageCode
) {
}

