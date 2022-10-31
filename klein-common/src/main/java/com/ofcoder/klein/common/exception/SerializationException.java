package com.ofcoder.klein.common.exception;

/**
 * @author far.liu
 */
public class SerializationException extends KleinException {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
