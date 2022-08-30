package com.ormanager.orm.exception;

public class DataConnectionException extends Exception {

    public DataConnectionException(String string) {
        super(string);
    }

    public DataConnectionException(String string, Throwable e) {
        super(string, e);
    }
}
