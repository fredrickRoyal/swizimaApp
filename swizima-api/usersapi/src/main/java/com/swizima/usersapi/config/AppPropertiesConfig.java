package com.swizima.usersapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        OtpProperties.class,
        AppMailProperties.class,
        SuperAdminSeedProperties.class,
        WhatsAppProperties.class
})
public class AppPropertiesConfig {
}
