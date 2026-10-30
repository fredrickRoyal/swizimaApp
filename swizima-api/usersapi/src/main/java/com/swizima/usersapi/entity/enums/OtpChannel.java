package com.swizima.usersapi.entity.enums;

/**
 * Channel used to deliver a one-time password.
 * SMS and WhatsApp are sent to the user's phone number; EMAIL to their email address.
 */
public enum OtpChannel {
    EMAIL,
    SMS,
    WHATSAPP
}
