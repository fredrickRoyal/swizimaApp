package com.swizima.whatsappapi.controller;

import com.swizima.whatsappapi.config.WhatsAppRelayProperties;
import com.swizima.whatsappapi.dto.WhatsAppBatchRequest;
import com.swizima.whatsappapi.dto.WhatsAppOtpRequest;
import com.swizima.whatsappapi.dto.WhatsAppSendRequest;
import com.swizima.whatsappapi.dto.WhatsAppSendResponse;
import com.swizima.whatsappapi.dto.WhatsAppTestRequest;
import com.swizima.whatsappapi.service.MetaWhatsAppRelayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Relay", description = "Standalone DigitalOcean WhatsApp Cloud API relay & webhook endpoints")
public class WhatsAppRelayController {

    private static final int MAX_RECENT_WEBHOOK_EVENTS = 50;

    private final MetaWhatsAppRelayService relayService;
    private final WhatsAppRelayProperties properties;
    private final Deque<Map<String, Object>> recentWebhookEvents = new ConcurrentLinkedDeque<>();

    @GetMapping("/health")
    @Operation(summary = "Public health probe for DigitalOcean load balancers and uptime checks")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "swizima-whatsappapi",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/config")
    @Operation(summary = "Inspect active Meta WhatsApp Cloud API configuration (secrets masked)")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(relayService.getConfigSummary());
    }

    @GetMapping("/webhook-setup")
    @Operation(summary = "Return the configured Meta Webhook Callback URL and Verify Token for Meta Developer Console")
    public ResponseEntity<Map<String, Object>> getWebhookSetup() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("callbackUrl", properties.meta().webhookCallbackUrl());
        info.put("verifyToken", properties.meta().webhookVerifyToken());
        info.put("subscribedFieldsRecommended", List.of("messages"));
        info.put("recentEventsCount", recentWebhookEvents.size());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/webhook-events")
    @Operation(summary = "List the most recent webhook events delivered by Meta Cloud API")
    public ResponseEntity<List<Map<String, Object>>> getRecentWebhookEvents() {
        return ResponseEntity.ok(new ArrayList<>(recentWebhookEvents));
    }

    @PostMapping("/test")
    @Operation(summary = "Send a test WhatsApp message (uses default 3p_direct_integration_test_template and default test number if body/params are omitted)")
    public ResponseEntity<WhatsAppSendResponse> sendTestPost(
            @RequestParam(name = "to", required = false) String queryTo,
            @RequestParam(name = "message", required = false) String queryMessage,
            @RequestParam(name = "templateName", required = false) String queryTemplate,
            @RequestParam(name = "languageCode", required = false) String queryLang,
            @RequestBody(required = false) WhatsAppTestRequest body) {
        WhatsAppTestRequest merged = new WhatsAppTestRequest(
                body != null && body.to() != null && !body.to().isBlank() ? body.to() : queryTo,
                body != null && body.message() != null && !body.message().isBlank() ? body.message() : queryMessage,
                body != null && body.templateName() != null && !body.templateName().isBlank() ? body.templateName() : queryTemplate,
                body != null && body.languageCode() != null && !body.languageCode().isBlank() ? body.languageCode() : queryLang
        );
        return ResponseEntity.ok(relayService.sendTestMessage(merged));
    }

    @GetMapping("/test")
    @Operation(summary = "Quick GET test endpoint to trigger a test WhatsApp message via query parameters")
    public ResponseEntity<WhatsAppSendResponse> sendTestGet(
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "templateName", required = false) String templateName,
            @RequestParam(name = "languageCode", required = false) String languageCode) {
        return ResponseEntity.ok(relayService.sendTestMessage(
                new WhatsAppTestRequest(to, message, templateName, languageCode)
        ));
    }

    @PostMapping("/send")
    @Operation(summary = "Send a WhatsApp template and/or text message via Meta Cloud API")
    public ResponseEntity<WhatsAppSendResponse> send(@Valid @RequestBody WhatsAppSendRequest request) {
        return ResponseEntity.ok(relayService.send(request));
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send a 6-digit OTP verification message via Meta Cloud API")
    public ResponseEntity<WhatsAppSendResponse> sendOtp(@Valid @RequestBody WhatsAppOtpRequest request) {
        return ResponseEntity.ok(relayService.sendOtp(request));
    }

    @PostMapping("/send-batch")
    @Operation(summary = "Send a WhatsApp notification to multiple supervisor phone numbers in one call")
    public ResponseEntity<Map<String, Object>> sendBatch(@Valid @RequestBody WhatsAppBatchRequest request) {
        return ResponseEntity.ok(relayService.sendBatch(request));
    }

    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Meta Cloud API Webhook Verification (GET)")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        String expected = properties.meta().webhookVerifyToken() != null
                ? properties.meta().webhookVerifyToken().strip()
                : "";
        String incoming = token != null ? token.strip() : "";
        if ("subscribe".equals(mode) && !expected.isEmpty() && expected.equals(incoming)) {
            log.info("Meta WhatsApp webhook verified successfully (challenge={})", challenge);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(challenge != null ? challenge : "");
        }
        log.warn("Meta WhatsApp webhook verification rejected (mode={}, tokenMatch={})", mode, expected.equals(incoming));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Webhook verification failed");
    }

    @PostMapping("/webhook")
    @Operation(summary = "Meta Cloud API Webhook Event Receiver (POST)")
    public ResponseEntity<Map<String, String>> receiveWebhook(@RequestBody Map<String, Object> payload) {
        Map<String, Object> entryRecord = new LinkedHashMap<>();
        entryRecord.put("receivedAt", Instant.now().toString());
        entryRecord.put("payload", payload);
        recentWebhookEvents.addFirst(entryRecord);
        while (recentWebhookEvents.size() > MAX_RECENT_WEBHOOK_EVENTS) {
            recentWebhookEvents.removeLast();
        }
        log.info("Received Meta WhatsApp webhook event: {}", payload);
        return ResponseEntity.ok(Map.of("status", "EVENT_RECEIVED"));
    }
}
