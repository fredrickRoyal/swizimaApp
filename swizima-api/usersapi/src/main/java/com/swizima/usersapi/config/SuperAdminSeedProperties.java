package com.swizima.usersapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Initial super admin account created on first start-up (see {@code SuperAdminSeeder}).
 *
 * @param enabled             whether seeding runs at all
 * @param email               login username and OTP email
 * @param phoneNumber         contact phone (any common format; normalised to E.164)
 * @param password            initial raw password (hashed with BCrypt before saving)
 * @param forcePasswordChange flag the password as system generated so the client prompts for a change on first login
 */
@ConfigurationProperties(prefix = "app.seed.super-admin")
public record SuperAdminSeedProperties(
        boolean enabled,
        String email,
        String phoneNumber,
        String password,
        boolean forcePasswordChange
) {
}
