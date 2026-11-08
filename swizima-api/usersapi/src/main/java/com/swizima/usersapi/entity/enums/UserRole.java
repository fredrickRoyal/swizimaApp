package com.swizima.usersapi.entity.enums;

/**
 * Platform role of a user.
 * <ul>
 *     <li>{@code SUPER_ADMIN} - full platform access; seeded on first start-up</li>
 *     <li>{@code CLIENT_ADMIN} - client business administrator (Client Portal)</li>
 *     <li>{@code USER} - regular user / client staff member (default)</li>
 *     <li>{@code ADMIN} - legacy alias for platform admin</li>
 * </ul>
 */
public enum UserRole {
    SUPER_ADMIN,
    CLIENT_ADMIN,
    USER,
    ADMIN
}
