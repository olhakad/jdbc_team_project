package com.ormanager.orm.exception;

public class OrmFieldTypeException extends RuntimeException {

    private final String message;
    private final Throwable cause;

    public OrmFieldTypeException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public OrmFieldTypeException(String message) {
        this(message, null);
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
