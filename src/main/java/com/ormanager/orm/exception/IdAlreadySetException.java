package com.ormanager.orm.exception;

public class IdAlreadySetException extends RuntimeException {
    private final String message;

    public IdAlreadySetException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
