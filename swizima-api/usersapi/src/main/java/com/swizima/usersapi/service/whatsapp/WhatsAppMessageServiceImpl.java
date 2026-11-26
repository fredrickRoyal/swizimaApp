package com.swizima.usersapi.service.whatsapp;

import com.swizima.usersapi.config.OtpProperties;
import com.swizima.usersapi.config.WhatsAppProperties;
import com.swizima.usersapi.dto.WhatsAppMessageRequest;
import com.swizima.usersapi.dto.WhatsAppMessageResponse;
import com.swizima.usersapi.exception.BadRequestException;
import com.swizima.usersapi.exception.OtpDeliveryException;
import com.swizima.usersapi.service.otp.OtpMasking;
import com.swizima.usersapi.util.UsernameNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link WhatsAppMessageService} supporting:
 * <ul>
 *     <li><b>META</b> — Meta WhatsApp Business Cloud API ({@code https://graph.facebook.com/{version}/{phoneNumberId}/messages})</li>
 *     <li><b>TWILIO</b> — Twilio Programmable Messaging API ({@code /Accounts/{AccountSid}/Messages.json})</li>
 *     <li><b>CUSTOM</b> — Any custom HTTP WhatsApp gateway</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppMessageServiceImpl implements WhatsAppMessageService {

    private final WhatsAppProperties whatsAppProperties;
    private final OtpProperties otpProperties;
    private final UsernameNormalizer usernameNormalizer;
    private final RestClient restClient = RestClient.create();

    @Override
    public WhatsAppMessageResponse sendMessage(WhatsAppMessageRequest request) {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new BadRequestException("Recipient phone number (to) is required");
        }
        boolean hasTemplate = request.templateName() != null && !request.templateName().isBlank();
        boolean hasText = request.message() != null && !request.message().isBlank();

        if (!hasTemplate && !hasText) {
            throw new BadRequestException("Either 'message' text or 'templateName' must be provided");
        }

        if (hasTemplate) {
            return sendTemplateMessage(
                    request.to(),
                    request.templateName().trim(),
                    request.languageCode(),
                    request.templateParameters()
            );
        }
        return sendTextMessage(request.to(), request.message().trim(), Boolean.TRUE.equals(request.previewUrl()));
    }

    @Override
    public WhatsAppMessageResponse sendTextMessage(String to, String message) {
        return sendTextMessage(to, message, false);
    }

    public WhatsAppMessageResponse sendTextMessage(String to, String message, boolean previewUrl) {
        String normalizedPhone = usernameNormalizer.normalizePhoneNumber(to);
        if (normalizedPhone == null) {
            throw new BadRequestException("Recipient phone number is required");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("WhatsApp message body is required");
        }

        if (!isWhatsAppEnabled()) {
            if (whatsAppProperties.logPayload()) {
                log.info("[WHATSAPP-SIMULATED] Text to {}: {}", OtpMasking.mask(normalizedPhone), message);
            }
            return new WhatsAppMessageResponse(
                    false,
                    true,
                    whatsAppProperties.provider().name(),
                    normalizedPhone,
                    "sim-" + UUID.randomUUID(),
                    "SIMULATED",
                    "WhatsApp provider is disabled (set app.whatsapp.enabled=true and provider credentials to deliver live messages).",
                    LocalDateTime.now()
            );
        }

        return switch (whatsAppProperties.provider()) {
            case META -> sendViaMetaText(normalizedPhone, message, previewUrl);
            case TWILIO -> sendViaTwilio(normalizedPhone, message);
            case CUSTOM -> sendViaCustomGateway(normalizedPhone, message, null, null, null);
        };
    }

    @Override
    public WhatsAppMessageResponse sendTemplateMessage(String to,
                                                       String templateName,
                                                       String languageCode,
                                                       List<String> parameters) {
        String normalizedPhone = usernameNormalizer.normalizePhoneNumber(to);
        if (normalizedPhone == null) {
            throw new BadRequestException("Recipient phone number is required");
        }
        String resolvedTemplate = (templateName != null && !templateName.isBlank())
                ? templateName.trim()
                : whatsAppProperties.meta().defaultTemplateName();
        if (resolvedTemplate == null || resolvedTemplate.isBlank()) {
            throw new BadRequestException("WhatsApp templateName is required");
        }
        String resolvedLang = (languageCode != null && !languageCode.isBlank())
                ? languageCode.trim()
                : whatsAppProperties.meta().defaultLanguageCode();

        if (!isWhatsAppEnabled()) {
            if (whatsAppProperties.logPayload()) {
                log.info("[WHATSAPP-SIMULATED] Template '{}' ({}) to {} params={}",
                        resolvedTemplate, resolvedLang, OtpMasking.mask(normalizedPhone), parameters);
            }
            return new WhatsAppMessageResponse(
                    false,
                    true,
                    whatsAppProperties.provider().name(),
                    normalizedPhone,
                    "sim-" + UUID.randomUUID(),
                    "SIMULATED",
                    "WhatsApp provider is disabled (set app.whatsapp.enabled=true and provider credentials to deliver live template messages).",
                    LocalDateTime.now()
            );
        }

        return switch (whatsAppProperties.provider()) {
            case META -> sendViaMetaTemplate(normalizedPhone, resolvedTemplate, resolvedLang, parameters);
            case TWILIO -> {
                String fallbackBody = parameters != null && !parameters.isEmpty()
                        ? String.join(" ", parameters)
                        : "Template: " + resolvedTemplate;
                yield sendViaTwilio(normalizedPhone, fallbackBody);
            }
            case CUSTOM -> sendViaCustomGateway(normalizedPhone, null, resolvedTemplate, resolvedLang, parameters);
        };
    }

    @Override
    public WhatsAppMessageResponse sendOtpMessage(String to, String code, int expiryMinutes) {
        String defaultTemplate = whatsAppProperties.meta().defaultTemplateName();
        if (whatsAppProperties.provider() == WhatsAppProperties.Provider.META
                && defaultTemplate != null
                && !defaultTemplate.isBlank()) {
            return sendTemplateMessage(
                    to,
                    defaultTemplate,
                    whatsAppProperties.meta().defaultLanguageCode(),
                    List.of(code)
            );
        }
        String textBody = "Your Swizima verification code is *" + code + "*. It expires in "
                + expiryMinutes + " minutes. Do not share this code with anyone.";
        return sendTextMessage(to, textBody, false);
    }

    @Override
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", isWhatsAppEnabled());
        summary.put("whatsappEnabledProperty", whatsAppProperties.enabled());
        summary.put("otpWhatsappEnabledProperty", otpProperties.whatsapp() != null && otpProperties.whatsapp().enabled());
        summary.put("provider", whatsAppProperties.provider().name());
        summary.put("defaultCountryCode", whatsAppProperties.defaultCountryCode());
        summary.put("logPayload", whatsAppProperties.logPayload());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("baseUrl", whatsAppProperties.meta().baseUrl());
        meta.put("apiVersion", whatsAppProperties.meta().apiVersion());
        meta.put("phoneNumberId", whatsAppProperties.meta().phoneNumberId());
        meta.put("businessAccountId", whatsAppProperties.meta().businessAccountId());
        meta.put("accessTokenConfigured", isNotBlank(whatsAppProperties.meta().accessToken()));
        meta.put("defaultTemplateName", whatsAppProperties.meta().defaultTemplateName());
        meta.put("defaultLanguageCode", whatsAppProperties.meta().defaultLanguageCode());
        summary.put("meta", meta);

        Map<String, Object> twilio = new LinkedHashMap<>();
        twilio.put("baseUrl", whatsAppProperties.twilio().baseUrl());
        twilio.put("accountSidConfigured", isNotBlank(whatsAppProperties.twilio().accountSid()));
        twilio.put("authTokenConfigured", isNotBlank(whatsAppProperties.twilio().authToken()));
        twilio.put("fromNumber", whatsAppProperties.twilio().fromNumber());
        summary.put("twilio", twilio);

        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("endpointUrl", whatsAppProperties.custom().endpointUrl());
        custom.put("apiKeyConfigured", isNotBlank(whatsAppProperties.custom().apiKey()));
        custom.put("authHeaderName", whatsAppProperties.custom().authHeaderName());
        summary.put("custom", custom);

        return summary;
    }

    // ------------------------------------------------------------------ Provider dispatchers

    private boolean isWhatsAppEnabled() {
        return whatsAppProperties.enabled()
                || (otpProperties.whatsapp() != null && otpProperties.whatsapp().enabled());
    }

    private WhatsAppMessageResponse sendViaMetaText(String e164Phone, String message, boolean previewUrl) {
        WhatsAppProperties.Meta meta = whatsAppProperties.meta();
        validateMetaConfig(meta);

        // Meta Cloud API expects digits without leading '+'
        String recipientDigits = e164Phone.startsWith("+") ? e164Phone.substring(1) : e164Phone;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientDigits);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "preview_url", previewUrl,
                "body", message
        ));

        return executeMetaRequest(e164Phone, meta, payload);
    }

    private WhatsAppMessageResponse sendViaMetaTemplate(String e164Phone,
                                                        String templateName,
                                                        String languageCode,
                                                        List<String> parameters) {
        WhatsAppProperties.Meta meta = whatsAppProperties.meta();
        validateMetaConfig(meta);

        String recipientDigits = e164Phone.startsWith("+") ? e164Phone.substring(1) : e164Phone;

        Map<String, Object> templateObj = new LinkedHashMap<>();
        templateObj.put("name", templateName);
        templateObj.put("language", Map.of("code", languageCode != null ? languageCode : "en_US"));

        if (parameters != null && !parameters.isEmpty()) {
            List<Map<String, Object>> paramObjects = new ArrayList<>();
            for (String p : parameters) {
                paramObjects.add(Map.of("type", "text", "text", p));
            }
            templateObj.put("components", List.of(
                    Map.of(
                            "type", "body",
                            "parameters", paramObjects
                    )
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", recipientDigits);
        payload.put("type", "template");
        payload.put("template", templateObj);

        return executeMetaRequest(e164Phone, meta, payload);
    }

    @SuppressWarnings("unchecked")
    private WhatsAppMessageResponse executeMetaRequest(String e164Phone,
                                                       WhatsAppProperties.Meta meta,
                                                       Map<String, Object> payload) {
        String url = String.format("%s/%s/%s/messages",
                trimTrailingSlash(meta.baseUrl()),
                meta.apiVersion(),
                meta.phoneNumberId().trim());

        try {
            Map<String, Object> responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + meta.accessToken().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String messageId = extractMetaMessageId(responseBody);
            log.info("WhatsApp message sent via META to {} (messageId={})", OtpMasking.mask(e164Phone), messageId);
            return new WhatsAppMessageResponse(
                    true,
                    false,
                    "META",
                    e164Phone,
                    messageId,
                    "SENT",
                    "Delivered to Meta WhatsApp Cloud API",
                    LocalDateTime.now()
            );
        } catch (RestClientResponseException ex) {
            log.error("Meta WhatsApp API error ({}) for {}: {}",
                    ex.getStatusCode(), OtpMasking.mask(e164Phone), ex.getResponseBodyAsString());
            throw new OtpDeliveryException(
                    "Meta WhatsApp API request failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message via META to {}: {}",
                    OtpMasking.mask(e164Phone), ex.getMessage());
            throw new OtpDeliveryException("Could not send WhatsApp message via Meta Cloud API: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private WhatsAppMessageResponse sendViaTwilio(String e164Phone, String message) {
        WhatsAppProperties.Twilio twilio = whatsAppProperties.twilio();
        if (!isNotBlank(twilio.accountSid()) || !isNotBlank(twilio.authToken()) || !isNotBlank(twilio.fromNumber())) {
            throw new OtpDeliveryException(
                    "Twilio WhatsApp credentials are incomplete (configure app.whatsapp.twilio.account-sid, auth-token, and from-number)");
        }

        String url = String.format("%s/Accounts/%s/Messages.json",
                trimTrailingSlash(twilio.baseUrl()),
                twilio.accountSid().trim());

        String from = twilio.fromNumber().trim().startsWith("whatsapp:")
                ? twilio.fromNumber().trim()
                : "whatsapp:" + twilio.fromNumber().trim();
        String to = "whatsapp:" + e164Phone;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("From", from);
        formData.add("To", to);
        formData.add("Body", message);

        String basicAuth = Base64.getEncoder().encodeToString(
                (twilio.accountSid().trim() + ":" + twilio.authToken().trim()).getBytes(StandardCharsets.UTF_8)
        );

        try {
            Map<String, Object> responseBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            String sid = responseBody != null && responseBody.get("sid") != null
                    ? String.valueOf(responseBody.get("sid"))
                    : "twilio-" + UUID.randomUUID();
            log.info("WhatsApp message sent via TWILIO to {} (sid={})", OtpMasking.mask(e164Phone), sid);
            return new WhatsAppMessageResponse(
                    true,
                    false,
                    "TWILIO",
                    e164Phone,
                    sid,
                    "QUEUED",
                    "Delivered to Twilio WhatsApp API",
                    LocalDateTime.now()
            );
        } catch (RestClientResponseException ex) {
            log.error("Twilio WhatsApp API error ({}) for {}: {}",
                    ex.getStatusCode(), OtpMasking.mask(e164Phone), ex.getResponseBodyAsString());
            throw new OtpDeliveryException(
                    "Twilio WhatsApp API request failed (" + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message via TWILIO to {}: {}",
                    OtpMasking.mask(e164Phone), ex.getMessage());
            throw new OtpDeliveryException("Could not send WhatsApp message via Twilio: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private WhatsAppMessageResponse sendViaCustomGateway(String e164Phone,
                                                         String message,
                                                         String templateName,
                                                         String languageCode,
                                                         List<String> parameters) {
        WhatsAppProperties.Custom custom = whatsAppProperties.custom();
        if (!isNotBlank(custom.endpointUrl())) {
            throw new OtpDeliveryException(
                    "Custom WhatsApp gateway URL is not configured (set app.whatsapp.custom.endpoint-url)");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", e164Phone);
        if (message != null) payload.put("message", message);
        if (templateName != null) payload.put("templateName", templateName);
        if (languageCode != null) payload.put("languageCode", languageCode);
        if (parameters != null) payload.put("parameters", parameters);

        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(custom.endpointUrl().trim())
                    .contentType(MediaType.APPLICATION_JSON);

            if (isNotBlank(custom.apiKey())) {
                String headerName = isNotBlank(custom.authHeaderName())
                        ? custom.authHeaderName().trim()
                        : HttpHeaders.AUTHORIZATION;
                spec.header(headerName, custom.apiKey().trim());
            }

            Map<String, Object> responseBody = spec.body(payload).retrieve().body(Map.class);
            String msgId = responseBody != null && responseBody.get("messageId") != null
                    ? String.valueOf(responseBody.get("messageId"))
                    : "custom-" + UUID.randomUUID();
            log.info("WhatsApp message sent via CUSTOM gateway to {} (id={})", OtpMasking.mask(e164Phone), msgId);
            return new WhatsAppMessageResponse(
                    true,
                    false,
                    "CUSTOM",
                    e164Phone,
                    msgId,
                    "SENT",
                    "Delivered to custom WhatsApp gateway",
                    LocalDateTime.now()
            );
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message via CUSTOM gateway to {}: {}",
                    OtpMasking.mask(e164Phone), ex.getMessage());
            throw new OtpDeliveryException("Could not send WhatsApp message via custom gateway: " + ex.getMessage(), ex);
        }
    }

    private void validateMetaConfig(WhatsAppProperties.Meta meta) {
        if (!isNotBlank(meta.phoneNumberId()) || !isNotBlank(meta.accessToken())) {
            throw new OtpDeliveryException(
                    "Meta WhatsApp Business Cloud API credentials are incomplete (configure app.whatsapp.meta.phone-number-id and app.whatsapp.meta.access-token)");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMetaMessageId(Map<String, Object> responseBody) {
        if (responseBody == null) return "meta-" + UUID.randomUUID();
        Object messagesObj = responseBody.get("messages");
        if (messagesObj instanceof List<?> list && !list.isEmpty()) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map && map.get("id") != null) {
                return String.valueOf(map.get("id"));
            }
        }
        return "meta-" + UUID.randomUUID();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) return "";
        String clean = url.trim();
        return clean.endsWith("/") ? clean.substring(0, clean.length() - 1) : clean;
    }
}

