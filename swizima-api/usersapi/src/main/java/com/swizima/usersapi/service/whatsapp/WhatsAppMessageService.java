package com.swizima.usersapi.service.whatsapp;

import com.swizima.usersapi.dto.WhatsAppMessageRequest;
import com.swizima.usersapi.dto.WhatsAppMessageResponse;

import java.util.List;
import java.util.Map;

/**
 * Service for sending WhatsApp messages (text, templates, and OTP codes)
 * and inspecting/updating WhatsApp provider configurations.
 */
public interface WhatsAppMessageService {

    /**
     * Send a free-form text WhatsApp message to the given phone number.
     */
    WhatsAppMessageResponse sendTextMessage(String to, String message);

    /**
     * Send a template-based WhatsApp message with body parameters.
     */
    WhatsAppMessageResponse sendTemplateMessage(String to,
                                                String templateName,
                                                String languageCode,
                                                List<String> parameters);

    /**
     * Send a WhatsApp OTP verification code.
     */
    WhatsAppMessageResponse sendOtpMessage(String to, String code, int expiryMinutes);

    /**
     * Dispatch either a template message (if {@code request.templateName()} is present)
     * or a text message.
     */
    WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request);

    /**
     * Returns the current WhatsApp configuration summary (with secrets masked).
     */
    Map<String, Object> getConfigurationSummary();
}

