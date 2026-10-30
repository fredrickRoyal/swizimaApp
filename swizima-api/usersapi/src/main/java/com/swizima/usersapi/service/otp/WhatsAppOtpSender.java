package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.config.OtpProperties;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.exception.OtpDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WhatsApp OTP sender.
 * <p>
 * TODO: integrate the WhatsApp Business Cloud API (Meta) or a BSP such as Twilio using an approved
 * authentication template, then set {@code app.otp.whatsapp.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppOtpSender implements OtpSender {

    private final OtpProperties otpProperties;

    @Override
    public OtpChannel channel() {
        return OtpChannel.WHATSAPP;
    }

    @Override
    public void send(String destination, String code, int expiryMinutes) {
        if (!otpProperties.whatsapp().enabled()) {
            throw new OtpDeliveryException("WhatsApp delivery is not available yet. Please choose EMAIL.");
        }
        // Provider integration goes here.
        log.info("WhatsApp OTP queued for {}", OtpMasking.mask(destination));
        throw new OtpDeliveryException("WhatsApp provider is enabled but not implemented");
    }
}
