package com.swizima.clienttransactionsapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Billing (Configurations API) refused or could not process the per-transaction charge,
 * so the transaction is blocked. {@code code} lets the UI react, e.g. prompt a top-up
 * on {@code INSUFFICIENT_CREDIT}.
 */
@Getter
public class BillingException extends RuntimeException {

    public static final String INSUFFICIENT_CREDIT = "INSUFFICIENT_CREDIT";
    public static final String BILLING_NOT_CONFIGURED = "BILLING_NOT_CONFIGURED";
    public static final String BILLING_REJECTED = "BILLING_REJECTED";
    public static final String BILLING_UNAVAILABLE = "BILLING_UNAVAILABLE";

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public BillingException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : details;
    }
}
