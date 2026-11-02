package com.swizima.clienttransactionsapi.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/** Where the vehicle is in its service lifecycle. */
public enum VehicleClearanceStatus {
    IN_SERVICE,
    READY,
    CLEARED,
    RELEASED;

    /**
     * Lenient JSON parsing: case-insensitive, accepts "ACTIVE"/"OPEN" as IN_SERVICE, and treats
     * blank or unknown values as "not provided" (null) instead of failing the whole request.
     * The service then keeps the current status (or defaults to IN_SERVICE on create).
     */
    @JsonCreator
    public static VehicleClearanceStatus fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (v) {
            case "ACTIVE", "OPEN", "INSERVICE" -> IN_SERVICE;
            default -> {
                try {
                    yield valueOf(v);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
