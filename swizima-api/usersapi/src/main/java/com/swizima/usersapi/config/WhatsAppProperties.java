package com.swizima.usersapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * WhatsApp messaging configuration bound from {@code app.whatsapp.*}.
 * Supports Meta WhatsApp Business Cloud API, Twilio WhatsApp API, or custom HTTP webhook/gateway.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("META") Provider provider,
        @DefaultValue("256") String defaultCountryCode,
        @DefaultValue("true") boolean logPayload,
        @DefaultValue Meta meta,
        @DefaultValue Twilio twilio,
        @DefaultValue Custom custom
) {

    public enum Provider {
        META,
        TWILIO,
        CUSTOM
    }

    /**
     * Meta WhatsApp Business Cloud API settings ({@code app.whatsapp.meta.*}).
     */
    public record Meta(
            @DefaultValue("https://graph.facebook.com") String baseUrl,
            @DefaultValue("v25.0") String apiVersion,
            @DefaultValue("1503783962810442") String phoneNumberId,
            @DefaultValue("") String businessAccountId,
            @DefaultValue("") String accessToken,
            @DefaultValue("3p_direct_integration_test_template") String defaultTemplateName,
            @DefaultValue("en_US") String defaultLanguageCode,
            @DefaultValue("swz_wa_whsec_9f4c2e8a71b6403d8e5f1a2c9b7d4e60") String webhookVerifyToken
    ) {
    }

    /**
     * Twilio WhatsApp API settings ({@code app.whatsapp.twilio.*}).
     */
    public record Twilio(
            @DefaultValue("https://api.twilio.com/2010-04-01") String baseUrl,
            @DefaultValue("") String accountSid,
            @DefaultValue("") String authToken,
            @DefaultValue("whatsapp:+14155238886") String fromNumber
    ) {
    }

    /**
     * Custom HTTP WhatsApp gateway settings ({@code app.whatsapp.custom.*}).
     */
    public record Custom(
            @DefaultValue("") String endpointUrl,
            @DefaultValue("") String apiKey,
            @DefaultValue("Authorization") String authHeaderName
    ) {
    }
}

