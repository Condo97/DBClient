package com.dbclient.exception;

public class DBManagerException extends Exception {

    public DBManagerException(String message) {
        super(message);
    }

    public DBManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DBManagerException(Throwable cause) {
        super(cause);
    }

}
