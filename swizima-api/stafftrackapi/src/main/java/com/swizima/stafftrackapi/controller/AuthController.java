package com.swizima.stafftrackapi.controller;

import com.swizima.stafftrackapi.entity.ClientUserAssignment;
import com.swizima.stafftrackapi.entity.Organisation;
import com.swizima.stafftrackapi.entity.StaffMember;
import com.swizima.stafftrackapi.entity.UserAccount;
import com.swizima.stafftrackapi.entity.enums.RecordStatus;
import com.swizima.stafftrackapi.exception.BusinessRuleException;
import com.swizima.stafftrackapi.exception.ResourceNotFoundException;
import com.swizima.stafftrackapi.repository.ClientUserAssignmentRepository;
import com.swizima.stafftrackapi.repository.OrganisationRepository;
import com.swizima.stafftrackapi.repository.StaffMemberRepository;
import com.swizima.stafftrackapi.repository.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/stafftrack/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & OTP Sign-In", description = "Two-step OTP sign-in backed by User Management and Staff Register")
public class AuthController {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final UserAccountRepository userAccountRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ClientUserAssignmentRepository clientUserRepository;
    private final OrganisationRepository organisationRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestClient restClient = RestClient.create();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:swizimaapp@gmail.com}")
    private String smtpUsername;

    @Value("${app.mail.from:no-reply@swizima.com}")
    private String mailFrom;

    @Value("${app.mail.from-name:Staff Out-of-Office notification}")
    private String mailFromName;

    @Value("${app.users-api.base-url:http://localhost:8085}")
    private String usersApiBaseUrl;

    private final Map<String, PendingOtpRecord> otpChallenges = new ConcurrentHashMap<>();

    public static final class PendingOtpRecord {
        private final String otpReference;
        private final String identifier;
        private final String maskedIdentifier;
        private final String loginMode;
        private final String channel;
        private final String code;
        private final long expiresAtMs;
        private final UUID userId;
        private final UUID staffId;
        private final String fullName;
        private final String email;
        private final String phone;
        private final String role;
        private final String designation;
        private final String department;
        private final String dutyStation;
        private int attempts;

        public PendingOtpRecord(String otpReference,
                                String identifier,
                                String maskedIdentifier,
                                String loginMode,
                                String channel,
                                String code,
                                long expiresAtMs,
                                UUID userId,
                                UUID staffId,
                                String fullName,
                                String email,
                                String phone,
                                String role,
                                String designation,
                                String department,
                                String dutyStation) {
            this.otpReference = otpReference;
            this.identifier = identifier;
            this.maskedIdentifier = maskedIdentifier;
            this.loginMode = loginMode;
            this.channel = channel;
            this.code = code;
            this.expiresAtMs = expiresAtMs;
            this.userId = userId;
            this.staffId = staffId;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.designation = designation;
            this.department = department;
            this.dutyStation = dutyStation;
            this.attempts = 0;
        }
    }

    public record OtpRequest(
            @NotBlank String identifier,
            String loginMode,
            String channel
    ) {
    }

    public record OtpChallengeResponse(
            String otpReference,
            String identifier,
            String maskedIdentifier,
            String loginMode,
            String channel,
            long expiresAt,
            String fullName,
            String role,
            String department,
            String message
    ) {
    }

    public record VerifyOtpRequest(
            String otpReference,
            String identifier,
            @NotBlank String code
    ) {
    }

    public record AuthSessionResponse(
            String id,
            String staffId,
            String fullName,
            String initials,
            String email,
            String phone,
            String role,
            String systemRole,
            String designation,
            String department,
            String dutyStation,
            String organisationId,
            String organisationName,
            String organisationCode,
            String identifier,
            String loginMode,
            String signedInAt
    ) {
    }

    @PostMapping("/request-otp")
    @Operation(summary = "Step 1: Verify user email or phone in User Management / Staff Register and deliver a real 6-digit OTP")
    public ResponseEntity<OtpChallengeResponse> requestOtp(@RequestBody OtpRequest request) {
        String rawId = request.identifier().trim();
        String mode = request.loginMode() != null
                ? request.loginMode().trim()
                : (rawId.contains("@") ? "email" : "phone");
        String requestedChannel = request.channel() != null
                ? request.channel().trim().toUpperCase()
                : (mode.equalsIgnoreCase("email") ? "EMAIL" : "WHATSAPP");

        ResolvedIdentity identity = resolveIdentity(rawId);

        String otpRef = UUID.randomUUID().toString();
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        long expiresAt = System.currentTimeMillis() + OTP_EXPIRY_MINUTES * 60 * 1000L;

        // Determine actual destination based on channel (EMAIL uses identity.email(), WHATSAPP/SMS uses identity.phone())
        String effectiveChannel = requestedChannel;
        String destinationValue;
        String destinationMode;

        if ("EMAIL".equalsIgnoreCase(requestedChannel)) {
            if (identity.email() != null && !identity.email().isBlank()) {
                destinationValue = identity.email().trim();
                destinationMode = "email";
            } else if (identity.phone() != null && !identity.phone().isBlank()) {
                effectiveChannel = "WHATSAPP";
                destinationValue = identity.phone().trim();
                destinationMode = "phone";
            } else {
                throw new BusinessRuleException("No email address or phone number is registered for this user.");
            }
        } else {
            // WHATSAPP or SMS requested
            if (identity.phone() != null && !identity.phone().isBlank()) {
                destinationValue = identity.phone().trim();
                destinationMode = "phone";
            } else if (identity.email() != null && !identity.email().isBlank()) {
                effectiveChannel = "EMAIL";
                destinationValue = identity.email().trim();
                destinationMode = "email";
            } else {
                throw new BusinessRuleException("No phone number or email address is registered for this user.");
            }
        }

        String masked = maskIdentifier(destinationValue, destinationMode);

        // Deliver real OTP via Email and/or WhatsApp
        deliverOtpCode(identity, otpCode, effectiveChannel);

        // Invalidate any previous challenges for this identifier
        otpChallenges.entrySet().removeIf(e -> e.getValue().identifier.equalsIgnoreCase(rawId));

        PendingOtpRecord record = new PendingOtpRecord(
                otpRef,
                rawId,
                masked,
                mode,
                effectiveChannel,
                otpCode,
                expiresAt,
                identity.userId(),
                identity.staffId(),
                identity.fullName(),
                identity.email(),
                identity.phone(),
                identity.role(),
                identity.designation(),
                identity.department(),
                identity.dutyStation()
        );
        otpChallenges.put(otpRef, record);

        String deliveryMessage = "A 6-digit verification code has been sent via " + effectiveChannel + " to " + masked + ".";

        return ResponseEntity.ok(new OtpChallengeResponse(
                otpRef,
                rawId,
                masked,
                mode,
                effectiveChannel,
                expiresAt,
                identity.fullName(),
                identity.role(),
                identity.department(),
                deliveryMessage
        ));
    }

    @PostMapping("/verify-otp")
    @Transactional
    @Operation(summary = "Step 2: Validate the real 6-digit OTP code and create an authenticated user session")
    public ResponseEntity<AuthSessionResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        String cleanCode = request.code() != null ? request.code().trim() : "";
        if (!cleanCode.matches("\\d{6}")) {
            throw new BusinessRuleException("Please enter a valid 6-digit verification code.");
        }

        PendingOtpRecord record = null;
        if (request.otpReference() != null && !request.otpReference().isBlank()) {
            record = otpChallenges.get(request.otpReference().trim());
        }
        if (record == null && request.identifier() != null && !request.identifier().isBlank()) {
            String cleanId = request.identifier().trim();
            record = otpChallenges.values().stream()
                    .filter(r -> r.identifier.equalsIgnoreCase(cleanId))
                    .findFirst()
                    .orElse(null);
        }

        if (record == null) {
            throw new BusinessRuleException("Your verification session expired or was not found. Please request a new verification code.");
        }

        if (System.currentTimeMillis() > record.expiresAtMs) {
            otpChallenges.remove(record.otpReference);
            throw new BusinessRuleException("Your verification code has expired. Please click 'Resend code' to receive a new one.");
        }

        if (!cleanCode.equals(record.code)) {
            record.attempts += 1;
            int remaining = MAX_ATTEMPTS - record.attempts;
            if (remaining <= 0) {
                otpChallenges.remove(record.otpReference);
                throw new BusinessRuleException("Too many incorrect verification attempts. Please request a new code.");
            }
            throw new BusinessRuleException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        otpChallenges.remove(record.otpReference);

        if (record.userId != null) {
            userAccountRepository.findByIdAndRecordStatusNot(record.userId, RecordStatus.DELETED)
                    .ifPresent(u -> {
                        u.setLastLoginAt(LocalDate.now().toString());
                        userAccountRepository.save(u);
                    });
        }

        // Resolve organisation (defaulting to OPM)
        final String targetEmail = record.email;
        final String targetPhone = normalizePhone(record.phone);
        List<ClientUserAssignment> userOrgs = clientUserRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED)
                .stream()
                .filter(cu -> (targetEmail != null && targetEmail.equalsIgnoreCase(cu.getUserEmail()))
                        || (!targetPhone.isEmpty() && targetPhone.equals(normalizePhone(cu.getUserPhoneNumber()))))
                .toList();

        ClientUserAssignment defaultOrgLink = userOrgs.stream()
                .filter(ClientUserAssignment::isDefaultClient)
                .findFirst()
                .orElse(userOrgs.isEmpty() ? null : userOrgs.getFirst());

        Optional<Organisation> opmOrg = organisationRepository
                .findByCodeIgnoreCaseAndRecordStatusNot("OPM", RecordStatus.DELETED);

        String orgId = defaultOrgLink != null && defaultOrgLink.getClientRef() != null
                ? defaultOrgLink.getClientRef().toString()
                : opmOrg.map(o -> o.getId().toString()).orElse("OPM");
        String orgName = defaultOrgLink != null
                ? defaultOrgLink.getClientName()
                : opmOrg.map(Organisation::getName).orElse("Office of the Prime Minister (OPM)");
        String orgCode = defaultOrgLink != null
                ? defaultOrgLink.getClientCode()
                : opmOrg.map(Organisation::getCode).orElse("OPM");

        String initials = computeInitials(record.fullName);

        String sysRole = record.role != null && !record.role.isBlank() ? record.role : "STAFF_OFFICER";

        return ResponseEntity.ok(new AuthSessionResponse(
                record.userId != null ? record.userId.toString() : (record.staffId != null ? record.staffId.toString() : "USR-OPM"),
                record.staffId != null ? record.staffId.toString() : null,
                record.fullName,
                initials,
                record.email,
                record.phone,
                sysRole,
                sysRole,
                record.designation,
                record.department,
                record.dutyStation,
                orgId,
                orgName,
                orgCode,
                record.identifier,
                record.loginMode,
                Instant.now().toString()
        ));
    }

    private void deliverOtpCode(ResolvedIdentity identity, String code, String channel) {
        boolean sentAny = false;

        if ("EMAIL".equalsIgnoreCase(channel)) {
            if (identity.email() != null && !identity.email().isBlank()) {
                sendOtpEmail(identity.email().trim(), identity.fullName(), code);
                sentAny = true;
            }
            if (identity.phone() != null && !identity.phone().isBlank()) {
                // Also dispatch via WhatsApp in the background if user has a phone number
                sendOtpWhatsApp(identity.phone().trim(), identity.fullName(), code, false);
            }
        } else {
            // WHATSAPP or SMS requested
            if (identity.phone() != null && !identity.phone().isBlank()) {
                sendOtpWhatsApp(identity.phone().trim(), identity.fullName(), code, false);
                sentAny = true;
            }
            if (identity.email() != null && !identity.email().isBlank()) {
                // Always send a backup copy to the officer's registered email address so they receive the code reliably
                sendOtpEmail(identity.email().trim(), identity.fullName(), code);
                sentAny = true;
            }
        }

        if (!sentAny) {
            throw new BusinessRuleException("Could not deliver verification code. Please check your registered email or phone number.");
        }
    }

    private void sendOtpEmail(String toEmail, String fullName, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessRuleException("Email delivery is not configured on the server.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String effectiveFrom = (smtpUsername != null && smtpUsername.contains("@"))
                    ? smtpUsername.trim()
                    : mailFrom;
            helper.setFrom(effectiveFrom, mailFromName);
            helper.setTo(toEmail);
            helper.setSubject("Your SOOTR verification code: " + code);
            helper.setText(buildOtpPlainText(fullName, code), buildOtpHtml(fullName, code));
            mailSender.send(message);
            log.info("Sent 6-digit login OTP email to {} ({})", fullName, maskIdentifier(toEmail, "email"));
        } catch (Exception ex) {
            log.error("Failed to send login OTP email to {}: {}", toEmail, ex.getMessage());
            throw new BusinessRuleException("Could not send verification code to your email (" + ex.getMessage() + ").");
        }
    }

    private void sendOtpWhatsApp(String toPhone, String fullName, String code, boolean throwOnError) {
        String base = usersApiBaseUrl != null ? usersApiBaseUrl.trim() : "http://localhost:8085";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/v1/users/whatsapp/send";

        String messageText = "Your SOOTR (OPM Staff Out-of-Office Tracker) verification code is *" + code
                + "*. It expires in " + OTP_EXPIRY_MINUTES + " minutes. Do not share this code with anyone.";

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
            log.info("Dispatched 6-digit login OTP via WhatsApp to {} ({})", fullName, maskIdentifier(toPhone, "phone"));
        } catch (Exception ex) {
            log.warn("Failed to dispatch login OTP via WhatsApp to {}: {}", toPhone, ex.getMessage());
            if (throwOnError) {
                throw new BusinessRuleException("Could not send verification code via WhatsApp: " + ex.getMessage());
            }
        }
    }

    private String buildOtpPlainText(String fullName, String code) {
        return """
                Hello %s,

                Your SOOTR (Staff Out-of-Office Tracking & Reporting) verification code is: %s

                This code expires in %d minutes. Do not share it with anyone.
                """.formatted(fullName != null ? fullName : "Officer", code, OTP_EXPIRY_MINUTES);
    }

    private String buildOtpHtml(String fullName, String code) {
        String safeName = HtmlUtils.htmlEscape(fullName != null ? fullName : "Officer");
        String safeCode = HtmlUtils.htmlEscape(code);
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;border:1px solid #cbd5e1;overflow:hidden;">
                        <tr>
                          <td style="background:#005eb8;color:#ffffff;padding:18px 24px;">
                            <div style="font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">
                              Department of Refugees
                            </div>
                            <div style="font-size:18px;font-weight:bold;margin-top:4px;">
                              SOOTR Sign-In Verification Code
                            </div>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px 24px;">
                            <p style="margin:0 0 14px;font-size:15px;color:#334155;">
                              Hello <strong>%s</strong>,
                            </p>
                            <p style="margin:0 0 20px;font-size:14px;color:#475569;">
                              Use the 6-digit verification code below to complete your sign-in to SOOTR:
                            </p>
                            <div style="text-align:center;padding:16px 0;">
                              <span style="display:inline-block;font-family:monospace;font-size:32px;letter-spacing:8px;font-weight:bold;
                                           color:#0f172a;background:#f1f5f9;border:1px solid #cbd5e1;padding:12px 24px;border-radius:8px;">%s</span>
                            </div>
                            <p style="margin:20px 0 0;font-size:13px;color:#64748b;">
                              This code expires in <strong>%d minutes</strong>. Never share this code with anyone.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, safeCode, OTP_EXPIRY_MINUTES);
    }

    private record ResolvedIdentity(
            UUID userId,
            UUID staffId,
            String fullName,
            String email,
            String phone,
            String role,
            String designation,
            String department,
            String dutyStation
    ) {
    }

    private ResolvedIdentity resolveIdentity(String rawIdentifier) {
        String lower = rawIdentifier.trim().toLowerCase();
        String normPhone = normalizePhone(rawIdentifier);

        List<UserAccount> users = userAccountRepository
                .findAllByRecordStatusNotOrderByCreatedAtDesc(RecordStatus.DELETED);

        for (UserAccount u : users) {
            boolean emailMatch = u.getEmail() != null && u.getEmail().trim().toLowerCase().equals(lower);
            boolean usernameMatch = u.getUsername() != null && u.getUsername().trim().toLowerCase().equals(lower);
            boolean phoneMatch = !normPhone.isEmpty() && normalizePhone(u.getPhone()).equals(normPhone);
            if (emailMatch || usernameMatch || phoneMatch) {
                if ("SUSPENDED".equalsIgnoreCase(u.getStatus())) {
                    throw new BusinessRuleException("Your user account (" + u.getFullName() + ") is currently suspended.");
                }
                return new ResolvedIdentity(
                        u.getId(),
                        u.getStaffId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getRole(),
                        u.getDesignation(),
                        u.getDepartment(),
                        u.getDutyStation()
                );
            }
        }

        List<StaffMember> staffList = staffMemberRepository
                .findAllByRecordStatusNotOrderBySurnameAscLastNameAsc(RecordStatus.DELETED);

        for (StaffMember s : staffList) {
            boolean emailMatch = (s.getEmail() != null && s.getEmail().trim().toLowerCase().equals(lower))
                    || (s.getAlternativeEmail() != null && s.getAlternativeEmail().trim().toLowerCase().equals(lower));
            boolean phoneMatch = !normPhone.isEmpty()
                    && (normalizePhone(s.getPhone()).equals(normPhone)
                    || normalizePhone(s.getAlternativePhone()).equals(normPhone));
            if (emailMatch || phoneMatch) {
                return new ResolvedIdentity(
                        null,
                        s.getId(),
                        s.getSurname() + " " + s.getLastName(),
                        s.getEmail(),
                        s.getPhone(),
                        "STAFF_OFFICER",
                        s.getDesignation(),
                        s.getDepartment(),
                        s.getDutyStation()
                );
            }
        }

        throw new ResourceNotFoundException(
                "No registered user or staff account found for '" + rawIdentifier + "'. Please assign the user in User Management first.");
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0") && digits.length() == 10) {
            return "256" + digits.substring(1);
        }
        return digits;
    }

    private String maskIdentifier(String value, String mode) {
        if ("email".equalsIgnoreCase(mode) && value.contains("@")) {
            String[] parts = value.split("@", 2);
            String user = parts[0];
            String domain = parts[1];
            if (user.length() <= 2) {
                return (user.isEmpty() ? "*" : user.charAt(0)) + "***@" + domain;
            }
            return user.substring(0, 2) + "***" + user.charAt(user.length() - 1) + "@" + domain;
        }
        String digits = value.replaceAll("\\s+", "");
        if (digits.length() > 6) {
            return digits.substring(0, 4) + " ••• " + digits.substring(digits.length() - 3);
        }
        return value;
    }

    private String computeInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "ST";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.isEmpty() ? "ST" : sb.toString();
    }
}
