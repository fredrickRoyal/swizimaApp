package com.swizima.whatsappapi;

import com.swizima.whatsappapi.config.WhatsAppRelayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WhatsAppRelayProperties.class)
public class WhatsappApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsappApiApplication.class, args);
    }
}

