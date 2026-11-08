package com.swizima.paymentsapi.exception;

/** A downstream service (e.g. Configurations API) could not be reached (mapped to 503). */
public class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
