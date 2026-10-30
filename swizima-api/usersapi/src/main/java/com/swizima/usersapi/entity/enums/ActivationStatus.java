package com.swizima.usersapi.entity.enums;

/**
 * Account activation lifecycle.
 * <ul>
 *     <li>{@code PENDING_ACTIVATION} - created, user has not yet set their own password</li>
 *     <li>{@code ACTIVE} - fully usable account</li>
 *     <li>{@code SUSPENDED} - temporarily blocked by an administrator</li>
 *     <li>{@code LOCKED} - blocked for security reasons</li>
 *     <li>{@code DEACTIVATED} - permanently closed</li>
 * </ul>
 */
public enum ActivationStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    SUSPENDED,
    LOCKED,
    DEACTIVATED;

    /** Whether a user in this status may log in. */
    public boolean canLogin() {
        return this == PENDING_ACTIVATION || this == ACTIVE;
    }
}
