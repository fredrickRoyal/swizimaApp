package com.swizima.usersapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Sender identity for outgoing emails, bound from {@code app.mail.*}.
 * SMTP connection settings live under the standard {@code spring.mail.*} keys.
 */
@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        @DefaultValue("no-reply@swizima.com") String from,
        @DefaultValue("Swizima") String fromName
) {
}
