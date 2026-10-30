package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.entity.enums.OtpChannel;

/**
 * Delivers an OTP code over a specific channel. Add a new implementation (Spring bean)
 * to support another channel or provider.
 */
public interface OtpSender {

    OtpChannel channel();

    /**
     * @param destination email address or E.164 phone number
     * @param code        plain OTP code
     * @param expiryMinutes how long the code is valid, for the message text
     */
    void send(String destination, String code, int expiryMinutes);
}
