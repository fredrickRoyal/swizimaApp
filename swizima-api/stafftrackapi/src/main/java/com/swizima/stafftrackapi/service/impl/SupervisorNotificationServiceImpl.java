package com.swizima.stafftrackapi.service.impl;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;
import com.swizima.stafftrackapi.entity.ReportBackManagementAction;
import com.swizima.stafftrackapi.entity.Supervisor;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.repository.SupervisorRepository;
import com.swizima.stafftrackapi.service.SupervisorNotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
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
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupervisorNotificationServiceImpl implements SupervisorNotificationService {

    private final SupervisorRepository supervisorRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final OutOfOfficePdfGenerator pdfGenerator;
    private final RestClient restClient = RestClient.create();

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

    @Override
    public void notifyOutOfOfficeCreated(OutOfOfficeNotification notification) {
        if (!notificationsEnabled || notification == null) {
            return;
        }
        List<Supervisor> supervisors = getActiveSupervisors();
        if (supervisors.isEmpty()) {
            log.info("No active supervisors configured to notify for Out-of-Office {}", notification.getReferenceNumber());
            return;
        }

        String subject = String.format("New Out-of-Office Notification: %s (%s)",
                notification.getOfficerName(),
                notification.getDestination());

        String plainAndWhatsAppText = buildCreatedWhatsAppText(notification);
        String htmlBody = buildCreatedEmailHtml(notification);
        byte[] pdfBytes = pdfGenerator.generateReportPdf(notification);
        String pdfFilename = buildPdfFilename(notification);

        CompletableFuture.runAsync(() -> dispatchToAllSupervisors(
                supervisors,
                subject,
                plainAndWhatsAppText,
                htmlBody,
                pdfBytes,
                pdfFilename,
                "OUT_OF_OFFICE_CREATED",
                notification.getReferenceNumber()
        ));
    }

    @Override
    public void notifyReportBackSubmitted(OutOfOfficeNotification notification, OutOfOfficeReportBack reportBack) {
        if (!notificationsEnabled || notification == null || reportBack == null) {
            return;
        }
        List<Supervisor> supervisors = getActiveSupervisors();
        if (supervisors.isEmpty()) {
            log.info("No active supervisors configured to notify for Report Back {}", notification.getReferenceNumber());
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
                supervisors,
                subject,
                plainAndWhatsAppText,
                htmlBody,
                pdfBytes,
                pdfFilename,
                "REPORT_BACK_SUBMITTED",
                notification.getReferenceNumber()
        ));
    }

    private String buildPdfFilename(OutOfOfficeNotification n) {
        String ref = n.getReferenceNumber() != null ? n.getReferenceNumber() : "OOO-Report";
        String officer = n.getOfficerName() != null
                ? n.getOfficerName().trim().replaceAll("[^A-Za-z0-9]+", "_")
                : "Officer";
        return String.format("SOOTR_Report_%s_%s.pdf", ref, officer);
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

        for (Supervisor sup : supervisors) {
            String supLabel = sup.getName() != null && !sup.getName().isBlank()
                    ? sup.getName()
                    : (sup.getEmail() != null && !sup.getEmail().isBlank() ? sup.getEmail() : sup.getPhone());

            // 1. Send Email with attached View-Report PDF if supervisor has an email address
            if (sup.getEmail() != null && !sup.getEmail().isBlank()) {
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

            // 2. Send WhatsApp message via usersapi if supervisor has a phone number
            if (sup.getPhone() != null && !sup.getPhone().isBlank()) {
                sendWhatsAppToSupervisor(sup.getPhone().trim(), supLabel, plainAndWhatsAppText);
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

    private void sendWhatsAppToSupervisor(String toPhone,
                                          String supervisorName,
                                          String messageText) {
        String base = usersApiBaseUrl != null ? usersApiBaseUrl.trim() : "http://localhost:8085";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/v1/users/whatsapp/send";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", toPhone);
        payload.put("message", messageText);
        payload.put("previewUrl", false);

        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Supervisor WhatsApp notification dispatched via usersapi to {} ({})", supervisorName, toPhone);
        } catch (Exception ex) {
            log.error("Failed to dispatch supervisor WhatsApp notification to {} ({}) via {}: {}",
                    supervisorName, toPhone, url, ex.getMessage());
        }
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

    private String buildCreatedWhatsAppText(OutOfOfficeNotification n) {
        String viewUrl = String.format("%s/out-of-office/%s/view-report",
                trimSlash(portalBaseUrl), n.getId());
        String otherStaff = formatOtherAttendingStaffNames(n);
        String otherStaffLine = !otherStaff.isEmpty() ? "\nOther Staff Attending: " + otherStaff : "";
        return """
                *SOOTR · New Out-of-Office Notification (%s)*
                Staff name: *%s* (%s)%s
                Department / Station: %s · %s
                Engagement: %s
                Destination: *%s*
                Host / Organizer: %s
                Dates: %s to %s
                Purpose: %s
                Field Phone: %s
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
                              New Out-of-Office Notification (%s)
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <p style="margin:0 0 16px;font-size:14px;color:#334155;">
                              An Out-of-Office Notification has been submitted.
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
                                View Official Report in SOOTR
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
                *SOOTR · Report Back Submitted (%s)*
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
                                View Section A &amp; B Report in SOOTR
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

