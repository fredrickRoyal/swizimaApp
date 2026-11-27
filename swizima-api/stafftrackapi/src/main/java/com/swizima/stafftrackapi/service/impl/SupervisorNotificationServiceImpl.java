package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;
import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.entity.enums.UnitHeadApprovalStatus;
import com.swizima.stafftrackapi.repository.SupervisorRepository;
import com.swizima.stafftrackapi.service.NotificationChannelConfigService;
import com.swizima.stafftrackapi.service.SupervisorNotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisorNotificationServiceImpl implements SupervisorNotificationService {

    private final SupervisorRepository supervisorRepository;
    private final NotificationChannelConfigService channelConfigService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final OutOfOfficePdfGenerator pdfGenerator;
    private final RestClient restClient = buildRestClient();

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Value("${app.notifications.supervisors.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.mail.from:no-reply@swizima.com}")
    private String mailFrom;

    @Value("${app.mail.from-name:Staff Out-of-Office notification}")
    private String mailFromName;

    @Value("${spring.mail.username:swizimaapp@gmail.com}")
    private String smtpUsername;

    @Value("${app.users-api.base-url:http://localhost:8085}")
    private String usersApiBaseUrl;

    @Value("${app.notifications.portal-base-url:http://localhost:4300}")
    private String portalBaseUrl;

    @Value("${app.notifications.pdf-outbox-dir:/tmp/swizima-files/outbox-reports}")
    private String pdfOutboxDir;

    @Value("${app.whatsapp.service.enabled:true}")
    private boolean whatsAppServiceEnabled;

    @Value("${app.whatsapp.service.base-url:http://localhost:8088}")
    private String whatsAppServiceBaseUrl;

    @Value("${app.whatsapp.service.api-key:swz-wa-relay-2026-secret}")
    private String whatsAppServiceApiKey;

    @Value("${app.whatsapp.meta.enabled:false}")
    private boolean metaWhatsAppEnabled;

    @Value("${app.whatsapp.meta.base-url:https://graph.facebook.com}")
    private String metaBaseUrl;

    @Value("${app.whatsapp.meta.api-version:v25.0}")
    private String metaApiVersion;

    @Value("${app.whatsapp.meta.phone-number-id:1503783962810442}")
    private String metaPhoneNumberId;

    @Value("${app.whatsapp.meta.access-token:}")
    private String metaAccessToken;

    @Value("${app.whatsapp.meta.template-name:3p_direct_integration_test_template}")
    private String metaTemplateName;

    @Value("${app.whatsapp.meta.language-code:en_US}")
    private String metaLanguageCode;

    @Override
    public void notifyOutOfOfficeCreated(OutOfOfficeNotification notification) {
        if (!notificationsEnabled || notification == null) {
            return;
        }
        Supervisor selectedUnitHead = resolveSelectedUnitHeadRecipient(notification);
        List<Supervisor> recipients;
        boolean isUnitHeadStage;

        if (notification.getUnitHeadApprovalStatus() == UnitHeadApprovalStatus.NOT_REQUIRED) {
            recipients = getActiveSupervisorsByCategory("OVERALL_HEAD");
            isUnitHeadStage = false;
        } else if (selectedUnitHead != null) {
            recipients = List.of(selectedUnitHead);
            isUnitHeadStage = true;
        } else {
            List<Supervisor> unitHeads = getActiveSupervisorsByCategory("UNIT_HEAD");
            if (!unitHeads.isEmpty()) {
                recipients = unitHeads;
                isUnitHeadStage = true;
            } else {
                recipients = getActiveSupervisorsByCategory("OVERALL_HEAD");
                isUnitHeadStage = false;
            }
        }

        if (recipients.isEmpty()) {
            log.info("No Unit Head / active supervisors configured to notify for Out-of-Office {}",
                    notification.getReferenceNumber());
            return;
        }

        String subject = isUnitHeadStage
                ? String.format("Action Required: Staff Out-of-Office Notification - %s",
                notification.getOfficerName())
                : String.format("Staff Out-of-Office Notification: %s (%s)",
                notification.getOfficerName(),
                notification.getDestination());

        String plainAndWhatsAppText = isUnitHeadStage
                ? buildUnitHeadReviewWhatsAppText(notification)
                : buildCreatedWhatsAppText(notification);
        String htmlBody = isUnitHeadStage
                ? buildUnitHeadReviewEmailHtml(notification)
                : buildCreatedEmailHtml(notification);
        byte[] pdfBytes = pdfGenerator.generateReportPdf(notification);
        String pdfFilename = buildPdfFilename(notification);
        String eventType = isUnitHeadStage ? "OUT_OF_OFFICE_UNIT_HEAD_REVIEW" : "OUT_OF_OFFICE_CREATED";

        CompletableFuture.runAsync(() -> dispatchToAllSupervisors(
                recipients,
                subject,
                plainAndWhatsAppText,
                htmlBody,
                pdfBytes,
                pdfFilename,
                eventType,
                notification.getReferenceNumber()
        ));
    }

    @Override
    public void notifyOverallHeadsAfterUnitHeadAcceptance(OutOfOfficeNotification notification) {
        if (!notificationsEnabled || notification == null) {
            return;
        }
        List<Supervisor> overallHeads = getActiveSupervisorsByCategory("OVERALL_HEAD");
        if (overallHeads.isEmpty()) {
            log.info("No active Overall Head supervisors configured to notify after Unit Head acceptance for {}",
                    notification.getReferenceNumber());
            return;
        }

        String subject = String.format("Staff Out-of-Office Notification (Accepted by Unit Head): %s (%s)",
                notification.getOfficerName(),
                notification.getDestination());

        String plainAndWhatsAppText = buildOverallHeadFyiWhatsAppText(notification);
        String htmlBody = buildOverallHeadFyiEmailHtml(notification);
        byte[] pdfBytes = pdfGenerator.generateReportPdf(notification);
        String pdfFilename = buildPdfFilename(notification);

        CompletableFuture.runAsync(() -> dispatchToAllSupervisors(
                overallHeads,
                subject,
                plainAndWhatsAppText,
                htmlBody,
                pdfBytes,
                pdfFilename,
                "OUT_OF_OFFICE_ACCEPTED_BY_UNIT_HEAD",
                notification.getReferenceNumber()
        ));
    }

    @Override
    public void notifyReportBackSubmitted(OutOfOfficeNotification notification, OutOfOfficeReportBack reportBack) {
        if (!notificationsEnabled || notification == null || reportBack == null) {
            return;
        }
        List<Supervisor> recipients = new ArrayList<>();
        Supervisor selectedUnitHead = resolveSelectedUnitHeadRecipient(notification);
        if (selectedUnitHead != null) {
            recipients.add(selectedUnitHead);
        }
        recipients.addAll(getActiveSupervisorsByCategory("OVERALL_HEAD"));
        if (recipients.isEmpty()) {
            recipients.addAll(getActiveSupervisors());
        }

        if (recipients.isEmpty()) {
            log.info("No Unit Head or Overall Head supervisors configured to notify for Report Back {}",
                    notification.getReferenceNumber());
            return;
        }

        String subject = String.format("Report Back Submitted: %s (%s)",
                notification.getOfficerName(),
                notification.getDestination());

        String plainAndWhatsAppText = buildReportBackWhatsAppText(notification, reportBack);
        String htmlBody = buildReportBackEmailHtml(notification, reportBack);
        byte[] pdfBytes = pdfGenerator.generateReportPdf(notification);
        String pdfFilename = buildPdfFilename(notification);

        CompletableFuture.runAsync(() -> dispatchToAllSupervisors(
                recipients,
                subject,
                plainAndWhatsAppText,
                htmlBody,
                pdfBytes,
                pdfFilename,
                "REPORT_BACK_SUBMITTED",
                notification.getReferenceNumber()
        ));
    }

    private Supervisor resolveSelectedUnitHeadRecipient(OutOfOfficeNotification n) {
        Supervisor dbSup = null;
        if (n.getUnitHeadSupervisorId() != null) {
            dbSup = supervisorRepository
                    .findByIdAndRecordStatusNot(n.getUnitHeadSupervisorId(), RecordStatus.DELETED)
                    .filter(s -> !"INACTIVE".equalsIgnoreCase(s.getStatus()))
                    .orElse(null);
        }
        String name = firstNonBlank(n.getUnitHeadName(), dbSup != null ? dbSup.getName() : null);
        String email = firstNonBlank(n.getUnitHeadEmail(), dbSup != null ? dbSup.getEmail() : null);
        String phone = firstNonBlank(n.getUnitHeadPhone(), dbSup != null ? dbSup.getPhone() : null);

        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            return null;
        }
        return Supervisor.builder()
                .id(dbSup != null ? dbSup.getId() : n.getUnitHeadSupervisorId())
                .name(name != null ? name : "Unit Head")
                .email(email)
                .phone(phone)
                .category("UNIT_HEAD")
                .status("ACTIVE")
                .build();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private List<Supervisor> getActiveSupervisorsByCategory(String targetCategory) {
        return getActiveSupervisors().stream()
                .filter(s -> {
                    String cat = s.getCategory();
                    boolean isUnitHead = cat != null && ("UNIT_HEAD".equalsIgnoreCase(cat.trim())
                            || "SUPERVISOR".equalsIgnoreCase(cat.trim())
                            || "UNIT_SUPERVISOR".equalsIgnoreCase(cat.trim()));
                    String normalized = isUnitHead ? "UNIT_HEAD" : "OVERALL_HEAD";
                    return normalized.equalsIgnoreCase(targetCategory);
                })
                .toList();
    }

    private String buildPdfFilename(OutOfOfficeNotification n) {
        String ref = n.getReferenceNumber() != null ? n.getReferenceNumber() : "OOO-Report";
        String officer = n.getOfficerName() != null
                ? n.getOfficerName().trim().replaceAll("[^A-Za-z0-9]+", "_")
                : "Officer";
        return String.format("Out_Of_Office_Report_%s_%s.pdf", ref, officer);
    }

    private List<Supervisor> getActiveSupervisors() {
        return supervisorRepository.findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(s -> !"INACTIVE".equalsIgnoreCase(s.getStatus()))
                .toList();
    }

    private void dispatchToAllSupervisors(List<Supervisor> supervisors,
                                          String emailSubject,
                                          String plainAndWhatsAppText,
                                          String emailHtml,
                                          byte[] pdfAttachmentBytes,
                                          String pdfFilename,
                                          String eventType,
                                          String referenceNumber) {
        log.info("Dispatching {} notifications for {} to {} active supervisor(s) via Email (with PDF '{}') & WhatsApp",
                eventType, referenceNumber, supervisors.size(), pdfFilename);

        savePdfToLocalOutbox(pdfAttachmentBytes, pdfFilename);

        boolean emailChannelActive = channelConfigService.isSupervisorChannelActive("EMAIL");
        boolean whatsAppChannelActive = channelConfigService.isSupervisorChannelActive("WHATSAPP");

        Set<String> sentEmails = new HashSet<>();
        Set<String> sentPhones = new HashSet<>();

        for (Supervisor sup : supervisors) {
            String supLabel = sup.getName() != null && !sup.getName().isBlank()
                    ? sup.getName()
                    : (sup.getEmail() != null && !sup.getEmail().isBlank() ? sup.getEmail() : sup.getPhone());

            // 1. Send Email with attached View-Report PDF if EMAIL channel is active and supervisor has an email address
            if (emailChannelActive && sup.getEmail() != null && !sup.getEmail().isBlank()) {
                String normEmail = sup.getEmail().trim().toLowerCase(Locale.ROOT);
                if (sentEmails.add(normEmail)) {
                    sendEmailToSupervisor(
                            sup.getEmail().trim(),
                            supLabel,
                            emailSubject,
                            plainAndWhatsAppText,
                            emailHtml,
                            pdfAttachmentBytes,
                            pdfFilename
                    );
                }
            }

            // 2. Send WhatsApp message if WHATSAPP channel is active and supervisor has a phone number
            if (whatsAppChannelActive && sup.getPhone() != null && !sup.getPhone().isBlank()) {
                String normPhone = normalizeWhatsAppDigits(sup.getPhone());
                if (!normPhone.isEmpty() && sentPhones.add(normPhone)) {
                    sendWhatsAppToSupervisor(sup.getPhone().trim(), supLabel, plainAndWhatsAppText);
                }
            }
        }
    }

    private void savePdfToLocalOutbox(byte[] pdfBytes, String pdfFilename) {
        if (pdfBytes == null || pdfBytes.length == 0 || pdfFilename == null) {
            return;
        }
        try {
            Path dir = Paths.get(pdfOutboxDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(pdfFilename);
            Files.write(target, pdfBytes);
            log.info("Saved generated report PDF to local outbox: {}", target);
        } catch (Exception ex) {
            log.debug("Could not write PDF to outbox {}: {}", pdfOutboxDir, ex.getMessage());
        }
    }

    private void sendEmailToSupervisor(String toEmail,
                                       String supervisorName,
                                       String subject,
                                       String plainText,
                                       String htmlBody,
                                       byte[] pdfAttachmentBytes,
                                       String pdfFilename) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                String effectiveFrom = (smtpUsername != null && smtpUsername.contains("@"))
                        ? smtpUsername.trim()
                        : mailFrom;
                helper.setFrom(effectiveFrom, mailFromName);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(plainText, htmlBody);

                if (pdfAttachmentBytes != null && pdfAttachmentBytes.length > 0) {
                    helper.addAttachment(
                            pdfFilename != null ? pdfFilename : "SOOTR_Out_Of_Office_Report.pdf",
                            new ByteArrayResource(pdfAttachmentBytes),
                            "application/pdf"
                    );
                }

                mailSender.send(mimeMessage);
                log.info("Supervisor email notification (with PDF {}) sent to {} ({})",
                        pdfFilename, supervisorName, toEmail);
                return;
            } catch (MailAuthenticationException authEx) {
                log.warn("Direct SMTP authentication failed in stafftrackapi for '{}' -> attempting relay via usersapi /api/v1/users/email/send",
                        smtpUsername);
            } catch (Exception ex) {
                log.warn("Direct SMTP send failed in stafftrackapi for {} ({}): {} -> attempting relay via usersapi",
                        supervisorName, toEmail, ex.getMessage());
            }
        }

        // Fallback: Relay email (with Base64 PDF attachment) through usersapi (/api/v1/users/email/send)
        sendEmailViaUsersApi(toEmail, supervisorName, subject, plainText, htmlBody, pdfAttachmentBytes, pdfFilename);
    }

    @SuppressWarnings("unchecked")
    private void sendEmailViaUsersApi(String toEmail,
                                      String supervisorName,
                                      String subject,
                                      String plainText,
                                      String htmlBody,
                                      byte[] pdfAttachmentBytes,
                                      String pdfFilename) {
        String base = usersApiBaseUrl != null ? usersApiBaseUrl.trim() : "http://localhost:8085";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/v1/users/email/send";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", toEmail);
        payload.put("subject", subject);
        payload.put("plainText", plainText);
        payload.put("htmlBody", htmlBody);
        payload.put("fromName", mailFromName);
        if (pdfAttachmentBytes != null && pdfAttachmentBytes.length > 0) {
            payload.put("attachmentFilename", pdfFilename != null ? pdfFilename : "SOOTR_Out_Of_Office_Report.pdf");
            payload.put("attachmentContentType", "application/pdf");
            payload.put("attachmentBase64", Base64.getEncoder().encodeToString(pdfAttachmentBytes));
        }

        try {
            Map<String, Object> resp = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            boolean delivered = resp != null && Boolean.TRUE.equals(resp.get("delivered"));
            if (delivered) {
                log.info("Supervisor email notification (with PDF {}) delivered via usersapi to {} ({})",
                        pdfFilename, supervisorName, toEmail);
            } else {
                String detail = resp != null && resp.get("detail") != null
                        ? String.valueOf(resp.get("detail"))
                        : "SMTP credentials rejected by mail server";
                log.warn("Supervisor email to {} ({}) queued in local outbox ({}/{}): {}",
                        supervisorName, toEmail, pdfOutboxDir, pdfFilename, detail);
            }
        } catch (Exception ex) {
            log.warn("Supervisor email to {} ({}) saved to local outbox ({}/{}) — SMTP / usersapi email relay unavailable: {}",
                    supervisorName, toEmail, pdfOutboxDir, pdfFilename, ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> notifyActiveSupervisorsViaWhatsApp(String customMessage) {
        List<Supervisor> supervisors = getActiveSupervisors();
        List<Map<String, Object>> results = new ArrayList<>();
        int sentCount = 0;

        for (Supervisor sup : supervisors) {
            if (sup.getPhone() == null || sup.getPhone().isBlank()) {
                continue;
            }
            String supLabel = sup.getName() != null && !sup.getName().isBlank()
                    ? sup.getName()
                    : (sup.getEmail() != null && !sup.getEmail().isBlank() ? sup.getEmail() : sup.getPhone());
            Map<String, Object> result = sendWhatsAppToSupervisor(sup.getPhone().trim(), supLabel, customMessage);
            if (Boolean.TRUE.equals(result.get("delivered"))) {
                sentCount++;
            }
            results.add(result);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeSupervisors", supervisors.size());
        summary.put("supervisorsWithPhone", results.size());
        summary.put("deliveredCount", sentCount);
        summary.put("results", results);
        return summary;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendWhatsAppToSupervisor(String toPhone,
                                                         String supervisorName,
                                                         String messageText) {
        Map<String, Object> outcome = new LinkedHashMap<>();
        outcome.put("supervisor", supervisorName);
        outcome.put("rawPhone", toPhone);

        String recipientDigits = normalizeWhatsAppDigits(toPhone);
        outcome.put("recipient", recipientDigits);
        if (recipientDigits.isEmpty()) {
            log.warn("Skipping WhatsApp notification for supervisor {} — invalid phone '{}'", supervisorName, toPhone);
            outcome.put("delivered", false);
            outcome.put("detail", "Invalid phone number");
            return outcome;
        }

        String resolvedTemplate = metaTemplateName != null && !metaTemplateName.isBlank()
                ? metaTemplateName.trim()
                : "hello_world";
        String resolvedLang = metaLanguageCode != null && !metaLanguageCode.isBlank()
                ? metaLanguageCode.trim()
                : "en_US";

        // 1. Primary: Dispatch directly to standalone DigitalOcean WhatsApp Relay Service (/api/v1/whatsapp/send)
        if (whatsAppServiceEnabled && whatsAppServiceBaseUrl != null && !whatsAppServiceBaseUrl.isBlank()) {
            String relayUrl = trimSlash(whatsAppServiceBaseUrl) + "/api/v1/whatsapp/send";
            Map<String, Object> relayPayload = new LinkedHashMap<>();
            relayPayload.put("to", recipientDigits);
            relayPayload.put("templateName", resolvedTemplate);
            relayPayload.put("languageCode", resolvedLang);
            if (messageText != null && !messageText.isBlank()) {
                relayPayload.put("message", messageText);
            }
            relayPayload.put("previewUrl", false);

            try {
                RestClient.RequestBodySpec reqSpec = restClient.post()
                        .uri(relayUrl)
                        .contentType(MediaType.APPLICATION_JSON);
                if (whatsAppServiceApiKey != null && !whatsAppServiceApiKey.isBlank()) {
                    reqSpec.header("X-API-Key", whatsAppServiceApiKey.trim());
                }
                Map<String, Object> relayResp = reqSpec.body(relayPayload).retrieve().body(Map.class);
                if (relayResp != null && Boolean.TRUE.equals(relayResp.get("delivered"))) {
                    log.info("Supervisor WhatsApp notification delivered via DigitalOcean Relay ({}) to {} ({})",
                            relayUrl, supervisorName, recipientDigits);
                    outcome.put("delivered", true);
                    outcome.put("provider", relayResp.getOrDefault("provider", "DIGITALOCEAN_RELAY"));
                    outcome.put("messageId", relayResp.get("messageId"));
                    outcome.put("via", "whatsappapi-relay");
                    return outcome;
                }
                log.debug("DigitalOcean WhatsApp Relay returned simulated/not-delivered for {}; checking fallback",
                        recipientDigits);
            } catch (Exception ex) {
                log.warn("DigitalOcean WhatsApp Relay ({}) unreachable for {} ({}): {} -> falling back to usersapi",
                        relayUrl, supervisorName, recipientDigits, ex.getMessage());
            }
        }

        // 2. Secondary: Dispatch via usersapi (/api/v1/users/whatsapp/send)
        String base = usersApiBaseUrl != null ? usersApiBaseUrl.trim() : "http://localhost:8085";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/v1/users/whatsapp/send";

        Map<String, Object> usersApiPayload = new LinkedHashMap<>();
        usersApiPayload.put("to", recipientDigits);
        usersApiPayload.put("templateName", resolvedTemplate);
        usersApiPayload.put("languageCode", resolvedLang);
        if (messageText != null && !messageText.isBlank()) {
            usersApiPayload.put("message", messageText);
        }
        usersApiPayload.put("previewUrl", false);

        try {
            Map<String, Object> resp = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(usersApiPayload)
                    .retrieve()
                    .body(Map.class);
            boolean delivered = resp != null && Boolean.TRUE.equals(resp.get("delivered"));
            if (delivered) {
                log.info("Supervisor WhatsApp notification delivered via usersapi to {} ({})",
                        supervisorName, recipientDigits);
                outcome.put("delivered", true);
                outcome.put("provider", resp.getOrDefault("provider", "META"));
                outcome.put("messageId", resp.get("messageId"));
                outcome.put("via", "usersapi");
                return outcome;
            }
            log.debug("usersapi WhatsApp response was simulated/not-delivered for {}; falling back to direct Meta Cloud API",
                    recipientDigits);
        } catch (Exception ex) {
            log.debug("usersapi WhatsApp endpoint unavailable ({}); falling back to direct Meta Cloud API for {}",
                    ex.getMessage(), recipientDigits);
        }

        // 2. Direct Meta WhatsApp Cloud API (https://graph.facebook.com/v25.0/1524050800784384/messages)
        if (!metaWhatsAppEnabled || metaAccessToken == null || metaAccessToken.isBlank()
                || metaPhoneNumberId == null || metaPhoneNumberId.isBlank()) {
            log.warn("Meta WhatsApp Cloud API credentials not configured for supervisor {} ({})",
                    supervisorName, recipientDigits);
            outcome.put("delivered", false);
            outcome.put("detail", "Meta WhatsApp Cloud API credentials not configured");
            return outcome;
        }

        String metaUrl = String.format("%s/%s/%s/messages",
                trimSlash(metaBaseUrl),
                metaApiVersion.trim(),
                metaPhoneNumberId.trim());

        Map<String, Object> templatePayload = new LinkedHashMap<>();
        templatePayload.put("messaging_product", "whatsapp");
        templatePayload.put("to", recipientDigits);
        templatePayload.put("type", "template");
        templatePayload.put("template", Map.of(
                "name", resolvedTemplate,
                "language", Map.of("code", resolvedLang)
        ));

        try {
            Map<String, Object> metaResp = restClient.post()
                    .uri(metaUrl)
                    .header("Authorization", "Bearer " + metaAccessToken.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(templatePayload)
                    .retrieve()
                    .body(Map.class);

            String messageId = null;
            if (metaResp != null && metaResp.get("messages") instanceof List<?> msgs && !msgs.isEmpty()
                    && msgs.get(0) instanceof Map<?, ?> firstMsg && firstMsg.get("id") != null) {
                messageId = String.valueOf(firstMsg.get("id"));
            }

            log.info("Supervisor WhatsApp template ('{}') sent via Meta Cloud API to {} ({}, messageId={})",
                    resolvedTemplate, supervisorName, recipientDigits, messageId);

            // Also send the detailed Out-of-Office / Report-Back text summary if a 24h customer window is open
            if (messageText != null && !messageText.isBlank()) {
                Map<String, Object> textPayload = new LinkedHashMap<>();
                textPayload.put("messaging_product", "whatsapp");
                textPayload.put("to", recipientDigits);
                textPayload.put("type", "text");
                textPayload.put("text", Map.of("preview_url", false, "body", messageText));
                try {
                    restClient.post()
                            .uri(metaUrl)
                            .header("Authorization", "Bearer " + metaAccessToken.trim())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(textPayload)
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                    // Template already delivered; free-form text succeeds when 24h reply window is active
                }
            }

            outcome.put("delivered", true);
            outcome.put("provider", "META");
            outcome.put("template", resolvedTemplate);
            outcome.put("messageId", messageId);
            outcome.put("via", "meta-direct");
            return outcome;
        } catch (Exception ex) {
            log.error("Failed to send Meta WhatsApp notification to supervisor {} ({}): {}",
                    supervisorName, recipientDigits, ex.getMessage());
            outcome.put("delivered", false);
            outcome.put("detail", ex.getMessage());
            return outcome;
        }
    }

    private String normalizeWhatsAppDigits(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return "";
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        } else if (digits.startsWith("0") && digits.length() == 10) {
            digits = "256" + digits.substring(1);
        } else if (digits.length() == 9 && digits.startsWith("7")) {
            digits = "256" + digits;
        }
        return digits;
    }

    // ------------------------------------------------------------------ Message Builders

    private String formatOtherAttendingStaffNames(OutOfOfficeNotification n) {
        if (!n.isAttendingWithOthers() || n.getAttendingOfficers() == null || n.getAttendingOfficers().isEmpty()) {
            return "";
        }
        return n.getAttendingOfficers().stream()
                .filter(ao -> ao.getName() != null && !ao.getName().isBlank())
                .map(ao -> {
                    String name = ao.getName().trim();
                    String desig = ao.getDesignation() != null && !ao.getDesignation().isBlank()
                            ? " (" + ao.getDesignation().trim() + ")"
                            : "";
                    return name + desig;
                })
                .collect(Collectors.joining(", "));
    }

    private String buildOtherAttendingStaffHtmlRow(OutOfOfficeNotification n) {
        String otherStaff = formatOtherAttendingStaffNames(n);
        if (otherStaff.isEmpty()) {
            return "";
        }
        return """
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Other Staff Attending</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                """.formatted(esc(otherStaff));
    }

    private String buildUnitHeadReviewWhatsAppText(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaff = formatOtherAttendingStaffNames(n);
        String otherStaffLine = !otherStaff.isEmpty() ? "\nOther Staff Attending: " + otherStaff : "";
        return """
                *Unit Head Review Required · Staff Out-of-Office Notification (%s)*
                Unit Head: *%s*
                Staff name: *%s* (%s)%s
                Department / Station: %s · %s
                Engagement: %s
                Destination: *%s*
                Host / Organizer: %s
                Dates: %s to %s
                Purpose: %s
                Field Phone: %s
                Action Required: Please view and Accept (or Reject) this notification so it can be forwarded to the Overall Head.
                Review & Accept: %s
                """.formatted(
                safe(n.getReferenceNumber()),
                safe(n.getUnitHeadName()),
                safe(n.getOfficerName()),
                safe(n.getOfficerRole()),
                otherStaffLine,
                safe(n.getDepartment()),
                safe(n.getDutyStation()),
                n.getEngagementType() != null ? n.getEngagementType().name() : "FIELD_ASSIGNMENT",
                safe(n.getDestination()),
                safe(n.getHostOrganization()),
                n.getStartDate(),
                n.getEndDate(),
                safe(n.getPurpose()),
                safe(n.getContactPhone()),
                viewUrl
        ).trim();
    }

    private String buildUnitHeadReviewEmailHtml(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaffRow = buildOtherAttendingStaffHtmlRow(n);
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;border:1px solid #cbd5e1;overflow:hidden;">
                        <tr>
                          <td style="background:#005eb8;color:#ffffff;padding:18px 24px;">
                            <div style="font-size:12px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">
                              Department of Refugees · Unit Head Review Required
                            </div>
                            <div style="font-size:18px;font-weight:bold;margin-top:4px;">
                              Staff Out-of-Office Notification (%s)
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 12px;font-size:14px;color:#334155;">
                              Dear <strong>%s</strong>, an Out-of-Office Notification has been submitted for your review as <strong>Unit Head</strong>.
                            </p>
                            <table width="100%%" cellpadding="8" cellspacing="0" style="border-collapse:collapse;font-size:13px;">
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;width:34%%;">Reference No.</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Staff name</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> (%s)</td>
                              </tr>
                              %s<tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Department &amp; Station</td>
                                <td style="border:1px solid #e2e8f0;">%s · %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Destination &amp; Host</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> — %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Scheduled Dates</td>
                                <td style="border:1px solid #e2e8f0;">%s → %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Purpose</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Contact Phone</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                            </table>
                            <div style="margin-top:20px;">
                              <a href="%s" style="display:inline-block;background:#005eb8;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:6px;font-size:13px;font-weight:bold;">
                                View &amp; Accept / Reject Notification
                              </a>
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                esc(n.getReferenceNumber()),
                esc(n.getUnitHeadName() != null && !n.getUnitHeadName().isBlank() ? n.getUnitHeadName() : "Unit Head"),
                esc(n.getReferenceNumber()),
                esc(n.getOfficerName()),
                esc(n.getOfficerRole()),
                otherStaffRow,
                esc(n.getDepartment()),
                esc(n.getDutyStation()),
                esc(n.getDestination()),
                esc(n.getHostOrganization()),
                esc(String.valueOf(n.getStartDate())),
                esc(String.valueOf(n.getEndDate())),
                esc(n.getPurpose()),
                esc(n.getContactPhone()),
                esc(viewUrl)
        );
    }

    private String buildOverallHeadFyiWhatsAppText(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaff = formatOtherAttendingStaffNames(n);
        String otherStaffLine = !otherStaff.isEmpty() ? "\nOther Staff Attending: " + otherStaff : "";
        String unitHeadReviewer = firstNonBlank(n.getUnitHeadReviewedBy(), n.getUnitHeadName());
        String commentLine = n.getUnitHeadComment() != null && !n.getUnitHeadComment().isBlank()
                ? "\nUnit Head Note: " + n.getUnitHeadComment().trim()
                : "";
        return """
                *Staff Out-of-Office Notification (%s) · Accepted by Unit Head*
                Staff name: *%s* (%s)%s
                Department / Station: %s · %s
                Engagement: %s
                Destination: *%s*
                Host / Organizer: %s
                Dates: %s to %s
                Purpose: %s
                Field Phone: %s
                Accepted by Unit Head: *%s*%s
                _For Overall Head information only — no accept or reject action required._
                View Report: %s
                """.formatted(
                safe(n.getReferenceNumber()),
                safe(n.getOfficerName()),
                safe(n.getOfficerRole()),
                otherStaffLine,
                safe(n.getDepartment()),
                safe(n.getDutyStation()),
                n.getEngagementType() != null ? n.getEngagementType().name() : "FIELD_ASSIGNMENT",
                safe(n.getDestination()),
                safe(n.getHostOrganization()),
                n.getStartDate(),
                n.getEndDate(),
                safe(n.getPurpose()),
                safe(n.getContactPhone()),
                safe(unitHeadReviewer),
                commentLine,
                viewUrl
        ).trim();
    }

    private String buildOverallHeadFyiEmailHtml(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaffRow = buildOtherAttendingStaffHtmlRow(n);
        String unitHeadReviewer = firstNonBlank(n.getUnitHeadReviewedBy(), n.getUnitHeadName());
        String commentSuffix = n.getUnitHeadComment() != null && !n.getUnitHeadComment().isBlank()
                ? " — <em>" + esc(n.getUnitHeadComment()) + "</em>"
                : "";
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;border:1px solid #cbd5e1;overflow:hidden;">
                        <tr>
                          <td style="background:#005eb8;color:#ffffff;padding:18px 24px;">
                            <div style="font-size:12px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">
                              Department of Refugees
                            </div>
                            <div style="font-size:18px;font-weight:bold;margin-top:4px;">
                              Staff Out-of-Office Notification (%s)
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 14px;font-size:14px;color:#334155;">
                              This Staff Out-of-Office Notification
                            </p>
                            <table width="100%%" cellpadding="8" cellspacing="0" style="border-collapse:collapse;font-size:13px;">
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;width:34%%;">Reference No.</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Staff name</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> (%s)</td>
                              </tr>
                              %s<tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Department &amp; Station</td>
                                <td style="border:1px solid #e2e8f0;">%s · %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Destination &amp; Host</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> — %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Scheduled Dates</td>
                                <td style="border:1px solid #e2e8f0;">%s → %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Purpose</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Accepted by Unit Head</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong>%s</td>
                              </tr>
                            </table>
                            <div style="margin-top:20px;">
                              <a href="%s" style="display:inline-block;background:#005eb8;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:6px;font-size:13px;font-weight:bold;">
                                View Official Report
                              </a>
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                esc(n.getReferenceNumber()),
                esc(n.getReferenceNumber()),
                esc(n.getOfficerName()),
                esc(n.getOfficerRole()),
                otherStaffRow,
                esc(n.getDepartment()),
                esc(n.getDutyStation()),
                esc(n.getDestination()),
                esc(n.getHostOrganization()),
                esc(String.valueOf(n.getStartDate())),
                esc(String.valueOf(n.getEndDate())),
                esc(n.getPurpose()),
                esc(unitHeadReviewer),
                commentSuffix,
                esc(viewUrl)
        );
    }

    private String buildCreatedWhatsAppText(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaff = formatOtherAttendingStaffNames(n);
        String otherStaffLine = !otherStaff.isEmpty() ? "\nOther Staff Attending: " + otherStaff : "";
        return """
                *Staff Out-of-Office Notification (%s)*
                Staff name: *%s* (%s)%s
                Department / Station: %s · %s
                Engagement: %s
                Destination: *%s*
                Host / Organizer: %s
                Dates: %s to %s
                Purpose: %s
                Field Phone: %s
                _For Overall Head information only — no accept or reject action required._
                View Report: %s
                """.formatted(
                safe(n.getReferenceNumber()),
                safe(n.getOfficerName()),
                safe(n.getOfficerRole()),
                otherStaffLine,
                safe(n.getDepartment()),
                safe(n.getDutyStation()),
                n.getEngagementType() != null ? n.getEngagementType().name() : "FIELD_ASSIGNMENT",
                safe(n.getDestination()),
                safe(n.getHostOrganization()),
                n.getStartDate(),
                n.getEndDate(),
                safe(n.getPurpose()),
                safe(n.getContactPhone()),
                viewUrl
        ).trim();
    }

    private String buildCreatedEmailHtml(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaffRow = buildOtherAttendingStaffHtmlRow(n);
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;border:1px solid #cbd5e1;overflow:hidden;">
                        <tr>
                          <td style="background:#005eb8;color:#ffffff;padding:18px 24px;">
                            <div style="font-size:12px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">
                              Department of Refugees
                            </div>
                            <div style="font-size:18px;font-weight:bold;margin-top:4px;">
                              Staff Out-of-Office Notification (%s)
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 16px;font-size:14px;color:#334155;">
                              This Staff Out-of-Office Notification
                            </p>
                            <table width="100%%" cellpadding="8" cellspacing="0" style="border-collapse:collapse;font-size:13px;">
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;width:34%%;">Reference No.</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Staff name</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> (%s)</td>
                              </tr>
                              %s<tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Department &amp; Station</td>
                                <td style="border:1px solid #e2e8f0;">%s · %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Destination &amp; Host</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> — %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Scheduled Dates</td>
                                <td style="border:1px solid #e2e8f0;">%s → %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Purpose</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Contact Phone</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                            </table>
                            <div style="margin-top:20px;">
                              <a href="%s" style="display:inline-block;background:#005eb8;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:6px;font-size:13px;font-weight:bold;">
                                View Official Report
                              </a>
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                esc(n.getReferenceNumber()),
                esc(n.getReferenceNumber()),
                esc(n.getOfficerName()),
                esc(n.getOfficerRole()),
                otherStaffRow,
                esc(n.getDepartment()),
                esc(n.getDutyStation()),
                esc(n.getDestination()),
                esc(n.getHostOrganization()),
                esc(String.valueOf(n.getStartDate())),
                esc(String.valueOf(n.getEndDate())),
                esc(n.getPurpose()),
                esc(n.getContactPhone()),
                esc(viewUrl)
        );
    }

    private String buildReportBackWhatsAppText(OutOfOfficeNotification n, OutOfOfficeReportBack rb) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String actionsSummary = (rb.getManagementActions() != null && !rb.getManagementActions().isEmpty())
                ? rb.getManagementActions().stream()
                .map(a -> a.getFollowUpAction() + " (" + a.getResponsibleOfficer() + ")")
                .collect(Collectors.joining("; "))
                : "None";
        String otherStaff = formatOtherAttendingStaffNames(n);
        String otherStaffLine = !otherStaff.isEmpty() ? "\nOther Staff Attending: " + otherStaff : "";

        return """
                *Staff Out-of-Office Tracker · Report Back Submitted (%s)*
                Staff name: *%s* (%s · %s)%s
                Destination: *%s* (%s)
                Actual Return Date: %s
                Stakeholders Met: %s
                Key Issues / Matters Discussed: %s
                Achievements / Outcomes: %s
                Management Actions: %s
                Attachments: %d file(s)
                View Full Report: %s
                """.formatted(
                safe(n.getReferenceNumber()),
                safe(n.getOfficerName()),
                safe(n.getOfficerRole()),
                safe(n.getDutyStation()),
                otherStaffLine,
                safe(n.getDestination()),
                safe(n.getHostOrganization()),
                rb.getActualReturnDate(),
                safe(rb.getStakeholdersMet()),
                safe(rb.getOutstandingIssues()),
                safe(rb.getWhatWasAchieved()),
                actionsSummary,
                rb.getAttachments() != null ? rb.getAttachments().size() : 0,
                viewUrl
        ).trim();
    }

    private String buildReportBackEmailHtml(OutOfOfficeNotification n, OutOfOfficeReportBack rb) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaffRow = buildOtherAttendingStaffHtmlRow(n);

        StringBuilder actionsHtml = new StringBuilder();
        if (rb.getManagementActions() != null && !rb.getManagementActions().isEmpty()) {
            for (ReportBackManagementAction ma : rb.getManagementActions()) {
                actionsHtml.append("<li><strong>")
                        .append(esc(ma.getFollowUpAction()))
                        .append("</strong> — ")
                        .append(esc(ma.getResponsibleOfficer()))
                        .append(ma.getTargetDate() != null ? " (Target: " + esc(ma.getTargetDate().toString()) + ")" : "")
                        .append("</li>");
            }
        } else {
            actionsHtml.append("<li>None recorded</li>");
        }

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:24px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;border:1px solid #cbd5e1;overflow:hidden;">
                        <tr>
                          <td style="background:#0f766e;color:#ffffff;padding:18px 24px;">
                            <div style="font-size:12px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">
                              Department of Refugees
                            </div>
                            <div style="font-size:18px;font-weight:bold;margin-top:4px;">
                              Post-Engagement Report Back Submitted (%s)
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 16px;font-size:14px;color:#334155;">
                              <strong>%s</strong> has submitted their Post-Engagement Report Back &amp; Management Actions:
                            </p>
                            <table width="100%%" cellpadding="8" cellspacing="0" style="border-collapse:collapse;font-size:13px;">
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;width:34%%;">Reference No.</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Staff name</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> (%s · %s)</td>
                              </tr>
                              %s<tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Destination &amp; Host</td>
                                <td style="border:1px solid #e2e8f0;"><strong>%s</strong> — %s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Actual Return Date</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Stakeholders Met</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Key Issues Discussed</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Deliverables Achieved</td>
                                <td style="border:1px solid #e2e8f0;">%s</td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Management Actions</td>
                                <td style="border:1px solid #e2e8f0;"><ul style="margin:0;padding-left:18px;">%s</ul></td>
                              </tr>
                              <tr>
                                <td style="border:1px solid #e2e8f0;background:#f8fafc;font-weight:bold;">Field Photos or PDF report</td>
                                <td style="border:1px solid #e2e8f0;">%d file(s) attached</td>
                              </tr>
                            </table>
                            <div style="margin-top:20px;">
                              <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:6px;font-size:13px;font-weight:bold;">
                                View Section A &amp; B Report
                              </a>
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                esc(n.getReferenceNumber()),
                esc(n.getOfficerName()),
                esc(n.getReferenceNumber()),
                esc(n.getOfficerName()),
                esc(n.getOfficerRole()),
                esc(n.getDutyStation()),
                otherStaffRow,
                esc(n.getDestination()),
                esc(n.getHostOrganization()),
                esc(String.valueOf(rb.getActualReturnDate())),
                esc(rb.getStakeholdersMet()),
                esc(rb.getOutstandingIssues()),
                esc(rb.getWhatWasAchieved()),
                actionsHtml.toString(),
                rb.getAttachments() != null ? rb.getAttachments().size() : 0,
                esc(viewUrl)
        );
    }

    private static String safe(String val) {
        return val != null && !val.isBlank() ? val.trim() : "—";
    }

    private static String esc(String val) {
        return HtmlUtils.htmlEscape(safe(val));
    }

    private static String trimSlash(String url) {
        if (url == null) return "http://localhost:4300";
        String c = url.trim();
        return c.endsWith("/") ? c.substring(0, c.length() - 1) : c;
    }
}

