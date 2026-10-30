package com.swizima.usersapi.config;

import com.swizima.usersapi.entity.enums.OtpChannel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * OTP settings bound from {@code app.otp.*}.
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        @DefaultValue("6") int length,
        @DefaultValue("5") int expiryMinutes,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("60") int resendCooldownSeconds,
        @DefaultValue("EMAIL") OtpChannel defaultChannel,
        /* Logs OTP codes to the console. LOCAL DEVELOPMENT ONLY - never enable in production. */
        @DefaultValue("false") boolean logCode,
        @DefaultValue Channel sms,
        @DefaultValue Channel whatsapp
) {

    public record Channel(@DefaultValue("false") boolean enabled) {
    }
}
