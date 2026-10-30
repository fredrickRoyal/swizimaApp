package com.swizima.usersapi.exception;

public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException(String message) {
        super(message);
    }
}
