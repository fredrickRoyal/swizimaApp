package com.swizima.usersapi.entity.enums;

/**
 * Platform role of a system user.
 * <ul>
 *     <li>{@code SUPER_ADMIN} - full platform access; seeded on first start-up</li>
 *     <li>{@code ADMIN} - administers users and configuration</li>
 *     <li>{@code USER} - regular user (default)</li>
 * </ul>
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    USER
}
