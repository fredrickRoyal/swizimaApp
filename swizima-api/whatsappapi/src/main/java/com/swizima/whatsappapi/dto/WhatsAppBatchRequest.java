package com.swizima.whatsappapi.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record WhatsAppBatchRequest(
        @NotEmpty(message = "At least one recipient phone number is required")
        List<String> recipients,
        String message,
        String templateName,
        String languageCode,
        List<String> templateParameters,
        Boolean previewUrl
) {
}

