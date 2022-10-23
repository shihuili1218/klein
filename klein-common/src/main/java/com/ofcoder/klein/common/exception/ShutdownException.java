package com.ofcoder.klein.common.exception;

/**
 * @author far.liu
 */
public class ShutdownException extends KleinException{
    public ShutdownException(String message) {
        super(message);
    }

    public ShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}
