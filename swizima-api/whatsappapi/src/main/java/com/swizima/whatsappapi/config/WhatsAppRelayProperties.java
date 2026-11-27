package com.swizima.whatsappapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppRelayProperties(
        @DefaultValue("true") boolean authRequired,
        @DefaultValue("swz-wa-relay-2026-secret") String serviceApiKey,
        @DefaultValue("true") boolean enabled,
        @DefaultValue("256") String defaultCountryCode,
        @DefaultValue("256700408387") String defaultTestRecipient,
        @DefaultValue("true") boolean logPayload,
        @DefaultValue Meta meta
) {

    public record Meta(
            @DefaultValue("https://graph.facebook.com") String baseUrl,
            @DefaultValue("v25.0") String apiVersion,
            @DefaultValue("1503783962810442") String phoneNumberId,
            @DefaultValue("") String businessAccountId,
            @DefaultValue("") String accessToken,
            @DefaultValue("3p_direct_integration_test_template") String defaultTemplateName,
            @DefaultValue("en_US") String defaultLanguageCode,
            @DefaultValue("swz_wa_whsec_9f4c2e8a71b6403d8e5f1a2c9b7d4e60") String webhookVerifyToken,
            @DefaultValue("https://swizima.com/api/v1/whatsapp/webhook") String webhookCallbackUrl,
            @DefaultValue("10000") int connectTimeoutMs,
            @DefaultValue("15000") int readTimeoutMs
    ) {
    }
}

