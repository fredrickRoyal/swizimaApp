package com.swizima.whatsappapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WhatsAppTestRequest(
        @Schema(description = "Recipient phone number (defaults to configured WHATSAPP_TEST_RECIPIENT if omitted)", example = "256700408387")
        String to,

        @Schema(description = "Optional custom text message to send alongside the test template", example = "Test message from Swizima DigitalOcean WhatsApp Relay")
        String message,

        @Schema(description = "Meta template name (defaults to 3p_direct_integration_test_template)", example = "3p_direct_integration_test_template")
        String templateName,

        @Schema(description = "Template language code (defaults to en_US)", example = "en_US")
        String languageCode
) {
}

