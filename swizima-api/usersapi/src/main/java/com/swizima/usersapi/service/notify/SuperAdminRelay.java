package com.swizima.usersapi.service.notify;

import com.swizima.usersapi.config.AppMailProperties;
import com.swizima.usersapi.entity.SystemUser;
import com.swizima.usersapi.entity.enums.RecordStatus;
import com.swizima.usersapi.entity.enums.UserRole;
import com.swizima.usersapi.repository.SystemUserRepository;
import com.swizima.usersapi.service.otp.OtpMasking;
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
import java.util.List;
import java.util.Objects;

/**
 * Emails super admins on behalf of users who can't be reached directly (no email, and
 * SMS/WhatsApp not available yet), so an admin can pass on a code or temporary password.
 * Disable with {@code app.notifications.relay-to-super-admin=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminRelay {

    private final SystemUserRepository userRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;

    @Value("${app.notifications.relay-to-super-admin:true}")
    private boolean enabled;

    /** Result: masked recipient list (e.g. "a***@x.com, b***@y.com"), or null when nothing was sent. */
    public String relayOtp(SystemUser user, String code, int expiryMinutes) {
        String who = HtmlUtils.htmlEscape(user.getUsername());
        return send(user.getUsername(),
                "Verification code for " + user.getUsername() + ": " + code,
                """
                %s is trying to sign in to Swizima but has no email address and SMS/WhatsApp delivery isn't available.

                Their verification code is: %s
                It expires in %d minutes. Only share it with %s after confirming who they are.
                """.formatted(user.getUsername(), code, expiryMinutes, user.getUsername()),
                body("Sign-in code for " + who,
                        "<strong>" + who + "</strong> is trying to sign in but can't receive codes directly.",
                        "Verification code", code,
                        "Expires in " + expiryMinutes + " minutes. Only share it after confirming who they are."));
    }

    public String relayCredentials(String username, String temporaryPassword, String organisation) {
        String org = organisation == null || organisation.isBlank() ? "Swizima" : organisation;
        String who = HtmlUtils.htmlEscape(username);
        return send(username,
                "New account for " + username + " - please forward",
                """
                An account was created for %s (%s), who has no email address.

                Username: %s
                Temporary password: %s

                Please pass these on securely. They'll choose their own password on first sign-in.
                """.formatted(username, org, username, temporaryPassword),
                body("New account for " + who,
                        "An account with access to <strong>" + HtmlUtils.htmlEscape(org) + "</strong> was created for <strong>"
                                + who + "</strong>, who has no email address. Please pass the details on securely.",
                        "Temporary password", temporaryPassword,
                        "Username: " + who + ". They'll choose their own password on first sign-in."));
    }

    private String send(String aboutUser, String subject, String plain, String html) {
        if (!enabled) return null;
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) return null;
        List<String> recipients = userRepository
                .findByRoleAndEnabledTrueAndRecordStatusNot(UserRole.SUPER_ADMIN, RecordStatus.DELETED).stream()
                .map(SystemUser::getEmail)
                .filter(Objects::nonNull)
                .filter(e -> !e.isBlank())
                .distinct()
                .toList();
        if (recipients.isEmpty()) {
            log.warn("No super admin with an email address to relay a message about {}", OtpMasking.mask(aboutUser));
            return null;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from(), mailProperties.fromName());
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(subject);
            helper.setText(plain, html);
            mailSender.send(message);
            log.info("Relayed message about {} to {} super admin(s)", OtpMasking.mask(aboutUser), recipients.size());
            return String.join(", ", recipients.stream().map(OtpMasking::mask).toList());
        } catch (Exception ex) {
            log.error("Failed to relay message about {} to super admins: {}", OtpMasking.mask(aboutUser), ex.getMessage());
            return null;
        }
    }

    private static String body(String title, String intro, String label, String secret, String footer) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;padding:32px;">
                        <tr><td style="font-size:20px;font-weight:bold;color:#111827;">%s</td></tr>
                        <tr><td style="padding-top:12px;font-size:15px;color:#374151;">%s</td></tr>
                        <tr><td style="padding-top:20px;font-size:14px;color:#374151;">%s:
                          <span style="font-family:monospace;font-size:18px;background:#eef2ff;padding:4px 10px;border-radius:6px;">%s</span>
                        </td></tr>
                        <tr><td style="padding-top:20px;font-size:13px;color:#6b7280;">%s</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(title, intro, label, HtmlUtils.htmlEscape(secret), footer);
    }
}
