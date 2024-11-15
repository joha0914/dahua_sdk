package org.example.dahuasdk.exceptions;

public class EventLoadingException extends RuntimeException {
    public EventLoadingException(String message) {
        super(message);
    }

    public EventLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
