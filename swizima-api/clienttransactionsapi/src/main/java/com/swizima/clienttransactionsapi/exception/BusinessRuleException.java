package com.swizima.clienttransactionsapi.exception;

/** A request that is well-formed but breaks a workflow rule (mapped to 409 Conflict). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
