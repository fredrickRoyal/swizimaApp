package com.swizima.usersapi.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates random temporary passwords that satisfy the password policy.
 * Ambiguous characters (0/O, 1/l/I) are excluded to make them easier to read from an SMS/email.
 */
@Component
public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "@#$%&*!?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public PasswordGenerator(@Value("${app.users.temp-password-length:12}") int length) {
        this.length = Math.max(length, 8);
    }

    public String generate() {
        List<Character> chars = new ArrayList<>(length);
        // Guarantee at least one of each class
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        while (chars.size() < length) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(length);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
