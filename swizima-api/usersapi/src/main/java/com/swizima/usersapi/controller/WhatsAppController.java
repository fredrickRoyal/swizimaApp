package com.swizima.usersapi.controller;

import com.swizima.usersapi.dto.WhatsAppMessageRequest;
import com.swizima.usersapi.dto.WhatsAppMessageResponse;
import com.swizima.usersapi.service.whatsapp.WhatsAppMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Messaging", description = "Send WhatsApp text/template messages and inspect WhatsApp provider configurations")
public class WhatsAppController {

    private final WhatsAppMessageService whatsAppMessageService;

    @GetMapping("/config")
    @Operation(summary = "Get current WhatsApp provider configuration summary (secrets masked)")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        return ResponseEntity.ok(whatsAppMessageService.getConfigurationSummary());
    }

    @PostMapping("/send")
    @Operation(summary = "Send a WhatsApp message (free-form text or template)")
    public ResponseEntity<WhatsAppMessageResponse> sendMessage(
            @Valid @RequestBody WhatsAppMessageRequest request) {
        return ResponseEntity.ok(whatsAppMessageService.sendMessage(request));
    }
}

