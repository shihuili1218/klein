package com.ofcoder.klein.rpc.facade.exception;

/**
 * @author 释慧利
 */
public class ConnectionException extends RpcException{
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
