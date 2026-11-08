package com.swizima.paymentsapi.exception;

/** The client has no billing plan in the Configurations API (mapped to 402, code BILLING_NOT_CONFIGURED). */
public class BillingNotConfiguredException extends RuntimeException {
    public BillingNotConfiguredException(String message) {
        super(message);
    }
}
