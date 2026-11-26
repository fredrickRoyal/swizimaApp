package com.swizima.usersapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request payload to send a WhatsApp message (plain text or template).
 */
public record WhatsAppMessageRequest(
        @NotBlank(message = "Recipient phone number (to) is required")
        String to,
        String message,
        String templateName,
        String languageCode,
        List<String> templateParameters,
        Boolean previewUrl
) {
}

