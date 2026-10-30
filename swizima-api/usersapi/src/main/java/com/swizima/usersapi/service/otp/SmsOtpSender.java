package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.config.OtpProperties;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.exception.OtpDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS OTP sender.
 * <p>
 * TODO: integrate an SMS provider (e.g. Africa's Talking, Twilio) and set {@code app.otp.sms.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsOtpSender implements OtpSender {

    private final OtpProperties otpProperties;

    @Override
    public OtpChannel channel() {
        return OtpChannel.SMS;
    }

    @Override
    public void send(String destination, String code, int expiryMinutes) {
        if (!otpProperties.sms().enabled()) {
            throw new OtpDeliveryException("SMS delivery is not available yet. Please choose EMAIL.");
        }
        // Provider integration goes here.
        String message = "Your Swizima code is %s. Expires in %d min. Do not share it.".formatted(code, expiryMinutes);
        log.info("SMS OTP queued for {} ({} chars)", OtpMasking.mask(destination), message.length());
        throw new OtpDeliveryException("SMS provider is enabled but not implemented");
    }
}
