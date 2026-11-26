package com.swizima.usersapi.dto;

import java.time.LocalDateTime;

/**
 * Result returned after sending (or simulating in dry-run/dev mode) a WhatsApp message.
 */
public record WhatsAppMessageResponse(
        boolean delivered,
        boolean simulated,
        String provider,
        String recipient,
        String messageId,
        String status,
        String detail,
        LocalDateTime timestamp
) {
}

