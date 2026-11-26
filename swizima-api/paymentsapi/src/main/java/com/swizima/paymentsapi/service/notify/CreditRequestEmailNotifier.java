package com.swizima.paymentsapi.service.notify;

import com.swizima.paymentsapi.entity.CreditTopUpRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * Sends an email notification to the Super Admin whenever a client submits a new credit top-up request.
 * Never throws so that request creation always succeeds even if SMTP is temporarily unreachable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditRequestEmailNotifier {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@swizima.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:Swizima}")
    private String fromName;

    @Value("${app.billing.super-admin-email:kfredrick35@gmail.com}")
    private String superAdminEmails;

    public void notifyNewRequest(CreditTopUpRequest req) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || superAdminEmails == null || superAdminEmails.isBlank()) {
            return;
        }
        String[] recipients = Arrays.stream(superAdminEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (recipients.length == 0) {
            return;
        }

        String clientLabel = req.getPayerName() != null && !req.getPayerName().isBlank()
                ? req.getPayerName()
                : req.getClientRef().toString();
        String amountText = NumberFormat.getNumberInstance(Locale.US).format(req.getRequestedAmount())
                + " " + req.getCurrency();
        String methodText = req.getPaymentMethod().name().replace('_', ' ');
        String referenceText = req.getPaymentReference() != null ? req.getPaymentReference() : "—";
        String phoneText = req.getPayerPhone() != null ? req.getPayerPhone() : "—";
        String notesText = req.getNotes() != null ? req.getNotes() : "—";

        String subject = "New Credit Top-Up Request: " + amountText + " from " + clientLabel;
        String plain = """
                A client has submitted a new credit top-up request on Swizima.

                Client: %s
                Client ID: %s
                Requested Amount: %s
                Payment Method: %s
                Payment Reference: %s
                Contact Phone: %s
                Notes: %s

                Sign in to the Swizima Admin Portal (/topup-requests) to approve and confirm the actual credit amount to top up, or reject the request.
                """.formatted(clientLabel, req.getClientRef(), amountText, methodText, referenceText, phoneText, notesText);

        String html = """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="520" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;padding:32px;border:1px solid #e5e7eb;">
                        <tr><td style="font-size:20px;font-weight:bold;color:#111827;">New Credit Top-Up Request</td></tr>
                        <tr><td style="padding-top:10px;font-size:15px;color:#374151;">
                          <strong>%s</strong> has requested a wallet credit top-up.
                        </td></tr>
                        <tr><td style="padding-top:18px;">
                          <table role="presentation" width="100%%" cellpadding="6" cellspacing="0"
                                 style="background:#f8fafc;border-radius:8px;padding:12px;font-size:14px;color:#1f2937;">
                            <tr><td style="color:#6b7280;">Requested amount:</td><td style="font-weight:bold;font-size:16px;color:#4f46e5;">%s</td></tr>
                            <tr><td style="color:#6b7280;">Payment method:</td><td><strong>%s</strong></td></tr>
                            <tr><td style="color:#6b7280;">Payment reference:</td><td style="font-family:monospace;">%s</td></tr>
                            <tr><td style="color:#6b7280;">Contact phone:</td><td>%s</td></tr>
                            <tr><td style="color:#6b7280;">Notes:</td><td>%s</td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding-top:20px;font-size:13px;color:#6b7280;">
                          Open <strong>Top-up requests</strong> in the Swizima Admin Portal to approve (and confirm the actual credit amount to load) or reject this request.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(clientLabel),
                HtmlUtils.htmlEscape(amountText),
                HtmlUtils.htmlEscape(methodText),
                HtmlUtils.htmlEscape(referenceText),
                HtmlUtils.htmlEscape(phoneText),
                HtmlUtils.htmlEscape(notesText)
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(plain, html);
            mailSender.send(message);
            log.info("Sent credit top-up request email notification for client {} ({}) to super admin",
                    req.getClientRef(), amountText);
        } catch (Exception ex) {
            log.error("Failed to send credit top-up request email notification for client {}: {}",
                    req.getClientRef(), ex.getMessage());
        }
    }
}

