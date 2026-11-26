package com.swizima.filesapi.globleexceptions;

public class MissingDataException extends RuntimeException {
    public MissingDataException(String message) {
        super(message);
    }
}
