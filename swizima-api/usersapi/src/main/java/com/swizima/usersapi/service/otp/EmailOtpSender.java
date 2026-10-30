package com.swizima.usersapi.service.otp;

import com.swizima.usersapi.config.AppMailProperties;
import com.swizima.usersapi.entity.enums.OtpChannel;
import com.swizima.usersapi.exception.OtpDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends OTP codes by email via SMTP ({@code spring.mail.*}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppMailProperties mailProperties;

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void send(String destination, String code, int expiryMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new OtpDeliveryException("Email delivery is not configured (set spring.mail.host)");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from(), mailProperties.fromName());
            helper.setTo(destination);
            helper.setSubject("Your Swizima verification code: " + code);
            helper.setText(plainText(code, expiryMinutes), html(code, expiryMinutes));
            mailSender.send(message);
            log.info("Login OTP email sent to {}", OtpMasking.mask(destination));
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send OTP email to {}: {}", OtpMasking.mask(destination), ex.getMessage());
            throw new OtpDeliveryException("Could not send verification code by email. Please try again.", ex);
        }
    }

    private String plainText(String code, int expiryMinutes) {
        return """
                Your Swizima verification code is: %s

                This code expires in %d minutes. Do not share it with anyone.
                If you did not try to sign in, please ignore this email and consider changing your password.
                """.formatted(code, expiryMinutes);
    }

    private String html(String code, int expiryMinutes) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;padding:32px;">
                        <tr><td style="font-size:20px;font-weight:bold;color:#0f172a;">Swizima</td></tr>
                        <tr><td style="padding-top:16px;font-size:15px;color:#334155;">
                          Use the code below to complete your sign in:
                        </td></tr>
                        <tr><td align="center" style="padding:24px 0;">
                          <span style="display:inline-block;font-size:32px;letter-spacing:8px;font-weight:bold;
                                       color:#0f172a;background:#f1f5f9;padding:12px 24px;border-radius:8px;">%s</span>
                        </td></tr>
                        <tr><td style="font-size:13px;color:#64748b;">
                          This code expires in <b>%d minutes</b>. Never share it with anyone.<br><br>
                          If you did not try to sign in, ignore this email and consider changing your password.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(code, expiryMinutes);
    }
}
