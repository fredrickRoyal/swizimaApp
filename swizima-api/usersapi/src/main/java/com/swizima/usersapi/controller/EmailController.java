package com.swizima.usersapi.controller;

import com.swizima.usersapi.config.AppMailProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/email")
@RequiredArgsConstructor
@Tag(name = "Email Messaging", description = "Send emails (with optional PDF attachment) and inspect SMTP configuration")
public class EmailController {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public record EmailSendRequest(
            @NotBlank(message = "Recipient email (to) is required")
            String to,
            @NotBlank(message = "Email subject is required")
            String subject,
            String plainText,
            String htmlBody,
            String fromName,
            String attachmentFilename,
            String attachmentContentType,
            String attachmentBase64
    ) {
    }

    public record EmailSendResponse(
            boolean delivered,
            String recipient,
            String subject,
            String status,
            String detail,
            LocalDateTime timestamp
    ) {
    }

    @GetMapping("/config")
    @Operation(summary = "Get current SMTP email configuration summary (password masked)")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("host", mailHost);
        summary.put("port", mailPort);
        summary.put("username", mailUsername);
        summary.put("passwordConfigured", mailPassword != null && !mailPassword.isBlank());
        summary.put("from", mailProperties.from());
        summary.put("fromName", mailProperties.fromName());
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/send")
    @Operation(summary = "Send an email (supports plain text, HTML body, and optional Base64 attachment)")
    public ResponseEntity<EmailSendResponse> sendEmail(@Valid @RequestBody EmailSendRequest request) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return ResponseEntity.ok(new EmailSendResponse(
                    false,
                    request.to(),
                    request.subject(),
                    "NOT_CONFIGURED",
                    "JavaMailSender is not configured (set spring.mail.host, MAIL_USERNAME, and MAIL_PASSWORD).",
                    LocalDateTime.now()
            ));
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            String fromName = (request.fromName() != null && !request.fromName().isBlank())
                    ? request.fromName().trim()
                    : mailProperties.fromName();
            helper.setFrom(mailProperties.from(), fromName);
            helper.setTo(request.to().trim());
            helper.setSubject(request.subject().trim());

            String plain = request.plainText() != null ? request.plainText() : "";
            String html = request.htmlBody() != null && !request.htmlBody().isBlank() ? request.htmlBody() : plain;
            helper.setText(plain, html);

            if (request.attachmentBase64() != null && !request.attachmentBase64().isBlank()) {
                byte[] bytes = Base64.getDecoder().decode(request.attachmentBase64());
                String filename = (request.attachmentFilename() != null && !request.attachmentFilename().isBlank())
                        ? request.attachmentFilename().trim()
                        : "attachment.pdf";
                String contentType = (request.attachmentContentType() != null && !request.attachmentContentType().isBlank())
                        ? request.attachmentContentType().trim()
                        : "application/pdf";
                helper.addAttachment(filename, new ByteArrayResource(bytes), contentType);
            }

            mailSender.send(mimeMessage);
            log.info("Email sent via usersapi to {} (subject='{}')", request.to(), request.subject());
            return ResponseEntity.ok(new EmailSendResponse(
                    true,
                    request.to(),
                    request.subject(),
                    "SENT",
                    "Email delivered via SMTP (" + mailHost + ":" + mailPort + ")",
                    LocalDateTime.now()
            ));
        } catch (MailAuthenticationException authEx) {
            log.warn("SMTP authentication failed for {} ({}): {}. Generate a valid 16-character Gmail App Password and set MAIL_USERNAME / MAIL_PASSWORD.",
                    mailUsername, mailHost, authEx.getMessage());
            return ResponseEntity.ok(new EmailSendResponse(
                    false,
                    request.to(),
                    request.subject(),
                    "SMTP_AUTH_FAILED",
                    "SMTP authentication failed for '" + mailUsername + "'. Update MAIL_USERNAME and MAIL_PASSWORD with a valid Gmail App Password.",
                    LocalDateTime.now()
            ));
        } catch (Exception ex) {
            log.error("Failed to send email via usersapi to {}: {}", request.to(), ex.getMessage());
            return ResponseEntity.ok(new EmailSendResponse(
                    false,
                    request.to(),
                    request.subject(),
                    "FAILED",
                    ex.getMessage(),
                    LocalDateTime.now()
            ));
        }
    }
}

