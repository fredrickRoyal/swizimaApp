package com.swizima.usersapi.entity.enums;

/**
 * Account category / login type on the Swizima platform.
 * <ul>
 *     <li>{@code SUPER_ADMIN} - Platform super administrator</li>
 *     <li>{@code CLIENT_ADMIN} - Client administrator (business owner / Client Portal admin)</li>
 *     <li>{@code USER} - Regular user / client staff member</li>
 * </ul>
 */
public enum AccountCategory {
    SUPER_ADMIN,
    CLIENT_ADMIN,
    USER
}
