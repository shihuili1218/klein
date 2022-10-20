package com.ofcoder.klein.rpc.facade.exception;

/**
 * @author far.liu
 */
public class SerializationException extends RpcException {
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
