package com.swizima.usersapi.util;

import com.swizima.usersapi.entity.enums.UsernameType;
import com.swizima.usersapi.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects whether a username is an email or a phone number and normalises it, so that
 * "John@Mail.com" == "john@mail.com" and "0700 123 456" == "+256700123456".
 */
@Component
public class UsernameNormalizer {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** E.164: '+' followed by 8-15 digits, first digit non-zero. */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private final String defaultCountryCode;

    public UsernameNormalizer(@Value("${app.users.default-country-code:256}") String defaultCountryCode) {
        this.defaultCountryCode = defaultCountryCode.replaceAll("\\D", "");
    }

    public record NormalizedUsername(String value, UsernameType type) {
    }

    public NormalizedUsername normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Username is required");
        }
        String value = raw.trim();

        if (value.contains("@")) {
            String email = value.toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(email).matches()) {
                throw new BadRequestException("Invalid email address: " + raw);
            }
            return new NormalizedUsername(email, UsernameType.EMAIL);
        }

        String phone = normalizePhone(value);
        if (!E164.matcher(phone).matches()) {
            throw new BadRequestException("Username must be a valid email address or phone number: " + raw);
        }
        return new NormalizedUsername(phone, UsernameType.PHONE);
    }

    /** Normalises and validates an email address. Returns null for blank input. */
    public String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Invalid email address: " + raw);
        }
        return email;
    }

    /** Normalises a phone number to E.164 and validates it. Returns null for blank input. */
    public String normalizePhoneNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String phone = normalizePhone(raw.trim());
        if (!E164.matcher(phone).matches()) {
            throw new BadRequestException("Invalid phone number: " + raw);
        }
        return phone;
    }

    private String normalizePhone(String value) {
        // Strip spaces, dashes, dots and brackets
        String cleaned = value.replaceAll("[\\s\\-().]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }
        if (cleaned.startsWith("0")) {
            // Local format, e.g. 0700123456 -> +256700123456
            return "+" + defaultCountryCode + cleaned.substring(1);
        }
        if (cleaned.startsWith(defaultCountryCode)) {
            return "+" + cleaned;
        }
        // Bare subscriber number without leading 0, e.g. 700123456
        return "+" + defaultCountryCode + cleaned;
    }
}
