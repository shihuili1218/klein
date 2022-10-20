package com.ofcoder.klein.common.exception;

/**
 * @author: 释慧利
 */
public class KleinException extends RuntimeException{
    public KleinException(String message) {
        super(message);
    }

    public KleinException(String message, Throwable cause) {
        super(message, cause);
    }
}
