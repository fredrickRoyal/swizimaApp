package com.swizima.usersapi.entity.enums;

public enum OtpStatus {
    /** Sent and awaiting verification. */
    PENDING,
    /** Successfully verified (single use). */
    VERIFIED,
    /** Replaced by a newer challenge (e.g. resend or new login). */
    INVALIDATED,
    /** Too many wrong attempts. */
    EXHAUSTED
}
