package com.swizima.usersapi.service.notify;

import com.swizima.usersapi.config.AppMailProperties;
import com.swizima.usersapi.service.otp.OtpMasking;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/**
 * Sends a newly created account's sign-in details to the user.
 * Only email is wired up for now (SMS/WhatsApp providers are not implemented yet).
 * Never throws: delivery problems are reported back so account creation still succeeds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCredentialsNotifier {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;
    private final SuperAdminRelay superAdminRelay;

    /**
     * Outcome shown to the admin. {@code sentTo} is masked; null when nothing was sent.
     * {@code viaSuperAdmin} is true when the user had no email and super admins were emailed instead.
     */
    public record Delivery(String sentTo, String note, boolean viaSuperAdmin) {
        Delivery(String sentTo, String note) {
            this(sentTo, note, false);
        }
    }

    public Delivery send(String email, String username, String temporaryPassword, String organisation) {
        if (email == null || email.isBlank()) {
            String admins = superAdminRelay.relayCredentials(username, temporaryPassword, organisation);
            return admins != null
                    ? new Delivery(admins, "The user has no email, so the details were sent to the super admin(s) to pass on.", true)
                    : new Delivery(null, "No email address on file - share the password with the user directly.");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return new Delivery(null, "Email delivery is not configured - share the password with the user directly.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from(), mailProperties.fromName());
            helper.setTo(email);
            helper.setSubject("Your Swizima account is ready");
            helper.setText(plainText(username, temporaryPassword, organisation), html(username, temporaryPassword, organisation));
            mailSender.send(message);
            log.info("Account credentials email sent to {}", OtpMasking.mask(email));
            return new Delivery(OtpMasking.mask(email), null);
        } catch (Exception ex) {
            log.error("Failed to send account credentials to {}: {}", OtpMasking.mask(email), ex.getMessage());
            return new Delivery(null, "Couldn't email the password - share it with the user directly.");
        }
    }

    private static String org(String organisation) {
        return organisation == null || organisation.isBlank() ? "Swizima" : organisation;
    }

    private String plainText(String username, String password, String organisation) {
        return """
                You've been given access to %s on Swizima.

                Username: %s
                Temporary password: %s

                You'll be asked to choose your own password when you first sign in.
                If you weren't expecting this, you can ignore this email.
                """.formatted(org(organisation), username, password);
    }

    private String html(String username, String password, String organisation) {
        String o = HtmlUtils.htmlEscape(org(organisation));
        String u = HtmlUtils.htmlEscape(username);
        String p = HtmlUtils.htmlEscape(password);
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;padding:32px;">
                        <tr><td style="font-size:20px;font-weight:bold;color:#111827;">Your Swizima account is ready</td></tr>
                        <tr><td style="padding-top:12px;font-size:15px;color:#374151;">
                          You've been given access to <strong>%s</strong>.
                        </td></tr>
                        <tr><td style="padding-top:20px;font-size:14px;color:#374151;">
                          Username: <strong>%s</strong><br/>
                          Temporary password:
                          <span style="font-family:monospace;font-size:16px;background:#eef2ff;padding:4px 8px;border-radius:6px;">%s</span>
                        </td></tr>
                        <tr><td style="padding-top:20px;font-size:13px;color:#6b7280;">
                          You'll be asked to choose your own password when you first sign in.
                          If you weren't expecting this, you can ignore this email.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(o, u, p);
    }
}
