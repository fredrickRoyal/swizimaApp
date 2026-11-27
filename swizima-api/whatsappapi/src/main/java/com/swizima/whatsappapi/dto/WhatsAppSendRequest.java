package com.swizima.whatsappapi.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Payload accepted by POST /api/v1/whatsapp/send (compatible with both usersapi custom gateway and stafftrackapi).
 */
public record WhatsAppSendRequest(
        @NotBlank(message = "Recipient phone number (to) is required")
        String to,
        String message,
        String templateName,
        String languageCode,
        List<String> templateParameters,
        List<String> parameters,
        Boolean previewUrl
) {
    public List<String> resolvedParameters() {
        if (templateParameters != null && !templateParameters.isEmpty()) {
            return templateParameters;
        }
        return parameters;
    }
}

