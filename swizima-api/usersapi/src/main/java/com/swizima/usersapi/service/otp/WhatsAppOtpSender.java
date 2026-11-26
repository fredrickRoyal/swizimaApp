package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.config.OtpProperties;
import com.swizima.usersapi.config.WhatsAppProperties;
import com.swizima.usersapi.dto.WhatsAppMessageResponse;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.exception.OtpDeliveryException;
import com.swizima.usersapi.service.whatsapp.WhatsAppMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WhatsApp OTP sender backed by {@link WhatsAppMessageService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppOtpSender implements OtpSender {

    private final OtpProperties otpProperties;
    private final WhatsAppProperties whatsAppProperties;
    private final WhatsAppMessageService whatsAppMessageService;

    @Override
    public OtpChannel channel() {
        return OtpChannel.WHATSAPP;
    }

    @Override
    public void send(String destination, String code, int expiryMinutes) {
        boolean enabled = whatsAppProperties.enabled()
                || (otpProperties.whatsapp() != null && otpProperties.whatsapp().enabled());
        if (!enabled) {
            throw new OtpDeliveryException(
                    "WhatsApp delivery is disabled. Set app.whatsapp.enabled=true (or OTP_WHATSAPP_ENABLED=true) and configure provider credentials.");
        }

        WhatsAppMessageResponse response = whatsAppMessageService.sendOtpMessage(destination, code, expiryMinutes);
        log.info("WhatsApp OTP sent to {} via {} (messageId={})",
                OtpMasking.mask(destination), response.provider(), response.messageId());
    }
}
