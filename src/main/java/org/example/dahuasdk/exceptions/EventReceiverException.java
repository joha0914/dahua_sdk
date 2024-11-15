package org.example.dahuasdk.exceptions;

public class EventReceiverException extends RuntimeException {
    public EventReceiverException(String message) {
        super(message);
    }

    public EventReceiverException(String message, Throwable cause) {
        super(message, cause);
    }
}
