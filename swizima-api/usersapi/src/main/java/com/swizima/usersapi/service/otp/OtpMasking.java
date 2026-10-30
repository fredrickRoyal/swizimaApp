package com.swizima.usersapi.service.otp;

/**
 * Masks email addresses and phone numbers for safe display and logging.
 * {@code jane.doe@mail.com -> ja******@mail.com}, {@code +256700123456 -> +256*******456}
 */
public final class OtpMasking {

    private OtpMasking() {
    }

    public static String mask(String destination) {
        if (destination == null || destination.isBlank()) {
            return "";
        }
        int at = destination.indexOf('@');
        if (at > 0) {
            String local = destination.substring(0, at);
            int visible = Math.min(2, local.length());
            return local.substring(0, visible) + "*".repeat(Math.max(local.length() - visible, 3))
                    + destination.substring(at);
        }
        if (destination.length() <= 7) {
            return "*".repeat(destination.length());
        }
        return destination.substring(0, 4)
                + "*".repeat(destination.length() - 7)
                + destination.substring(destination.length() - 3);
    }
}
