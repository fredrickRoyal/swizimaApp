package com.swizima.whatsappapi.service;

import com.swizima.whatsappapi.config.WhatsAppRelayProperties;
import com.swizima.whatsappapi.dto.WhatsAppBatchRequest;
import com.swizima.whatsappapi.dto.WhatsAppOtpRequest;
import com.swizima.whatsappapi.dto.WhatsAppSendRequest;
import com.swizima.whatsappapi.dto.WhatsAppSendResponse;
import com.swizima.whatsappapi.dto.WhatsAppTestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class MetaWhatsAppRelayService {

    private final WhatsAppRelayProperties properties;
    private final RestClient restClient;

    public MetaWhatsAppRelayService(WhatsAppRelayProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.meta().connectTimeoutMs());
        factory.setReadTimeout(properties.meta().readTimeoutMs());
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> getConfigSummary() {
        WhatsAppRelayProperties.Meta meta = properties.meta();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("service", "swizima-whatsappapi");
        summary.put("enabled", properties.enabled());
        summary.put("defaultCountryCode", properties.defaultCountryCode());
        summary.put("apiKeyProtected", isNotBlank(properties.serviceApiKey()));
        summary.put("metaBaseUrl", meta.baseUrl());
        summary.put("metaApiVersion", meta.apiVersion());
        summary.put("phoneNumberId", meta.phoneNumberId());
        summary.put("accessTokenConfigured", isNotBlank(meta.accessToken()));
        summary.put("defaultTemplateName", meta.defaultTemplateName());
        summary.put("defaultLanguageCode", meta.defaultLanguageCode());
        summary.put("webhookCallbackUrl", meta.webhookCallbackUrl());
        summary.put("webhookVerifyTokenConfigured", isNotBlank(meta.webhookVerifyToken()));
        summary.put("defaultTestRecipient", properties.defaultTestRecipient());
        return summary;
    }

    public WhatsAppSendResponse sendTestMessage(WhatsAppTestRequest request) {
        String rawTo = (request != null && isNotBlank(request.to()))
                ? request.to().trim()
                : properties.defaultTestRecipient();
        if (!isNotBlank(rawTo)) {
            throw new IllegalArgumentException("Test recipient phone number (to) is required");
        }

        String template = (request != null && isNotBlank(request.templateName()))
                ? request.templateName().trim()
                : properties.meta().defaultTemplateName();
        String lang = (request != null && isNotBlank(request.languageCode()))
                ? request.languageCode().trim()
                : properties.meta().defaultLanguageCode();
        String customText = (request != null && isNotBlank(request.message()))
                ? request.message().trim()
                : null;

        return send(new WhatsAppSendRequest(
                rawTo,
                customText,
                template,
                lang,
                null,
                null,
                false
        ));
    }

    public WhatsAppSendResponse send(WhatsAppSendRequest request) {
        String digits = normalizeDigits(request.to());
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Recipient phone number (to) is invalid: '" + request.to() + "'");
        }

        boolean hasTemplate = isNotBlank(request.templateName());
        boolean hasText = isNotBlank(request.message());

        if (!hasTemplate && !hasText) {
            // Default to the configured Meta template (e.g. hello_world)
            return sendTemplate(digits, properties.meta().defaultTemplateName(), properties.meta().defaultLanguageCode(), null);
        }

        if (hasTemplate) {
            List<String> params = isZeroParameterTemplate(request.templateName().trim())
                    ? null
                    : request.resolvedParameters();
            WhatsAppSendResponse templateResp = sendTemplate(
                    digits,
                    request.templateName().trim(),
                    request.languageCode(),
                    params
            );
            // If caller also supplied detailed notification text (e.g. Out-of-Office / Report-Back summary or OTP),
            // attempt sending the text message right after the template (delivers whenever 24h customer window is open).
            if (hasText && properties.enabled()) {
                try {
                    sendText(digits, request.message().trim(), Boolean.TRUE.equals(request.previewUrl()), false);
                } catch (Exception ex) {
                    log.debug("Optional follow-up WhatsApp text after template '{}' skipped for {}: {}",
                            request.templateName(), maskPhone(digits), ex.getMessage());
                }
            }
            return templateResp;
        }

        return sendText(digits, request.message().trim(), Boolean.TRUE.equals(request.previewUrl()), true);
    }

    public WhatsAppSendResponse sendOtp(WhatsAppOtpRequest request) {
        String digits = normalizeDigits(request.to());
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Recipient phone number (to) is invalid: '" + request.to() + "'");
        }
        int expiry = request.expiryMinutes() != null && request.expiryMinutes() > 0 ? request.expiryMinutes() : 5;
        String appLabel = isNotBlank(request.appName())
                ? request.appName().trim()
                : "Staff Out-of-Office Tracker";

        String resolvedTemplate = isNotBlank(request.templateName())
                ? request.templateName().trim()
                : properties.meta().defaultTemplateName();
        String resolvedLang = isNotBlank(request.languageCode())
                ? request.languageCode().trim()
                : properties.meta().defaultLanguageCode();

        String textBody = "Your " + appLabel + " verification code is *" + request.code().trim()
                + "*. It expires in " + expiry + " minutes. Do not share this code with anyone.";

        if (isNotBlank(resolvedTemplate)) {
            if (isZeroParameterTemplate(resolvedTemplate)) {
                WhatsAppSendResponse templateResp = sendTemplate(digits, resolvedTemplate, resolvedLang, null);
                if (properties.enabled()) {
                    try {
                        return sendText(digits, textBody, false, false);
                    } catch (Exception ex) {
                        log.debug("Follow-up OTP text after '{}' skipped for {}: {}", resolvedTemplate, maskPhone(digits), ex.getMessage());
                    }
                }
                return templateResp;
            }
            return sendTemplate(digits, resolvedTemplate, resolvedLang, List.of(request.code().trim()));
        }

        return sendText(digits, textBody, false, true);
    }

    public Map<String, Object> sendBatch(WhatsAppBatchRequest request) {
        List<WhatsAppSendResponse> results = new ArrayList<>();
        int deliveredCount = 0;

        for (String rawTo : request.recipients()) {
            if (rawTo == null || rawTo.isBlank()) {
                continue;
            }
            try {
                WhatsAppSendResponse res = send(new WhatsAppSendRequest(
                        rawTo,
                        request.message(),
                        request.templateName(),
                        request.languageCode(),
                        request.templateParameters(),
                        null,
                        request.previewUrl()
                ));
                if (res.delivered()) {
                    deliveredCount++;
                }
                results.add(res);
            } catch (Exception ex) {
                results.add(new WhatsAppSendResponse(
                        false,
                        false,
                        "META",
                        normalizeDigits(rawTo),
                        null,
                        "FAILED",
                        ex.getMessage(),
                        LocalDateTime.now()
                ));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requestedCount", request.recipients().size());
        summary.put("deliveredCount", deliveredCount);
        summary.put("results", results);
        return summary;
    }

    private WhatsAppSendResponse sendTemplate(String digits,
                                              String templateName,
                                              String languageCode,
                                              List<String> parameters) {
        String resolvedTemplate = isNotBlank(templateName)
                ? templateName.trim()
                : properties.meta().defaultTemplateName();
        String resolvedLang = isNotBlank(languageCode)
                ? languageCode.trim()
                : properties.meta().defaultLanguageCode();

        if (!properties.enabled()) {
            if (properties.logPayload()) {
                log.info("[WHATSAPP-RELAY-SIMULATED] Template '{}' ({}) to {}", resolvedTemplate, resolvedLang, maskPhone(digits));
            }
            return new WhatsAppSendResponse(
                    false,
                    true,
                    "META",
                    "+" + digits,
                    "sim-" + UUID.randomUUID(),
                    "SIMULATED",
                    "WhatsApp relay is in dry-run mode (set WHATSAPP_ENABLED=true).",
                    LocalDateTime.now()
            );
        }

        Map<String, Object> templateObj = new LinkedHashMap<>();
        templateObj.put("name", resolvedTemplate);
        templateObj.put("language", Map.of("code", resolvedLang));

        if (parameters != null && !parameters.isEmpty()) {
            List<Map<String, Object>> paramObjects = new ArrayList<>();
            for (String p : parameters) {
                paramObjects.add(Map.of("type", "text", "text", p));
            }
            templateObj.put("components", List.of(
                    Map.of("type", "body", "parameters", paramObjects)
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", digits);
        payload.put("type", "template");
        payload.put("template", templateObj);

        try {
            return postToMeta(digits, payload);
        } catch (IllegalStateException ex) {
            if (parameters != null && !parameters.isEmpty() && ex.getMessage() != null && ex.getMessage().contains("132000")) {
                log.warn("Template '{}' rejected body parameters (#132000); retrying with 0 parameters", resolvedTemplate);
                return sendTemplate(digits, resolvedTemplate, resolvedLang, null);
            }
            throw ex;
        }
    }

    private static boolean isZeroParameterTemplate(String name) {
        if (name == null) return false;
        String clean = name.trim();
        return "hello_world".equalsIgnoreCase(clean)
                || "3p_direct_integration_test_template".equalsIgnoreCase(clean);
    }

    private WhatsAppSendResponse sendText(String digits,
                                          String message,
                                          boolean previewUrl,
                                          boolean fallbackToTemplateOnError) {
        if (!properties.enabled()) {
            if (properties.logPayload()) {
                log.info("[WHATSAPP-RELAY-SIMULATED] Text to {}: {}", maskPhone(digits), message);
            }
            return new WhatsAppSendResponse(
                    false,
                    true,
                    "META",
                    "+" + digits,
                    "sim-" + UUID.randomUUID(),
                    "SIMULATED",
                    "WhatsApp relay is in dry-run mode (set WHATSAPP_ENABLED=true).",
                    LocalDateTime.now()
            );
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", digits);
        payload.put("type", "text");
        payload.put("text", Map.of("preview_url", previewUrl, "body", message));

        try {
            return postToMeta(digits, payload);
        } catch (RuntimeException ex) {
            if (fallbackToTemplateOnError && isNotBlank(properties.meta().defaultTemplateName())) {
                log.info("Text delivery failed for {}; falling back to default template '{}'",
                        maskPhone(digits), properties.meta().defaultTemplateName());
                return sendTemplate(digits, properties.meta().defaultTemplateName(), properties.meta().defaultLanguageCode(), null);
            }
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private WhatsAppSendResponse postToMeta(String digits, Map<String, Object> payload) {
        WhatsAppRelayProperties.Meta meta = properties.meta();
        if (!isNotBlank(meta.phoneNumberId()) || !isNotBlank(meta.accessToken())) {
            throw new IllegalStateException("Meta Cloud API credentials are incomplete (WHATSAPP_META_PHONE_NUMBER_ID / WHATSAPP_META_ACCESS_TOKEN)");
        }

        String baseUrl = meta.baseUrl() != null && meta.baseUrl().trim().endsWith("/")
                ? meta.baseUrl().trim().substring(0, meta.baseUrl().trim().length() - 1)
                : (meta.baseUrl() != null ? meta.baseUrl().trim() : "https://graph.facebook.com");

        String url = String.format("%s/%s/%s/messages",
                baseUrl,
                meta.apiVersion().trim(),
                meta.phoneNumberId().trim());

        try {
            Map<String, Object> respBody = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + meta.accessToken().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String msgId = extractMessageId(respBody);
            log.info("WhatsApp message relayed via Meta Cloud API to {} (messageId={})", maskPhone(digits), msgId);
            return new WhatsAppSendResponse(
                    true,
                    false,
                    "META",
                    "+" + digits,
                    msgId,
                    "SENT",
                    "Delivered to Meta WhatsApp Cloud API via DigitalOcean Relay",
                    LocalDateTime.now()
            );
        } catch (RestClientResponseException ex) {
            log.error("Meta Graph API error ({}) for {}: {}",
                    ex.getStatusCode(), maskPhone(digits), ex.getResponseBodyAsString());
            throw new IllegalStateException("Meta WhatsApp API error (" + ex.getStatusCode().value() + "): "
                    + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Failed to reach Meta WhatsApp Cloud API for {}: {}", maskPhone(digits), ex.getMessage());
            throw new IllegalStateException("Could not reach Meta WhatsApp Cloud API: " + ex.getMessage(), ex);
        }
    }

    public String normalizeDigits(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        String cc = isNotBlank(properties.defaultCountryCode()) ? properties.defaultCountryCode().replaceAll("\\D", "") : "256";
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 10) {
            digits = cc + digits.substring(1);
        } else if (digits.length() == 9 && digits.startsWith("7")) {
            digits = cc + digits;
        }
        return digits;
    }

    private String extractMessageId(Map<String, Object> body) {
        if (body != null && body.get("messages") instanceof List<?> list && !list.isEmpty()) {
            Object first = list.getFirst();
            if (first instanceof Map<?, ?> map && map.get("id") != null) {
                return String.valueOf(map.get("id"));
            }
        }
        return "meta-" + UUID.randomUUID();
    }

    private static String maskPhone(String digits) {
        if (digits == null || digits.length() < 6) return "***";
        return "+" + digits.substring(0, 3) + "****" + digits.substring(digits.length() - 3);
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

