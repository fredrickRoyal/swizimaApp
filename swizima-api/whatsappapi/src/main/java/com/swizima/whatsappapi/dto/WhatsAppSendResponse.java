package com.swizima.whatsappapi.dto;

import java.time.LocalDateTime;

public record WhatsAppSendResponse(
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

