package com.ofcoder.klein.rpc.facade.exception;

/**
 * @author 释慧利
 */
public class InvokeTimeoutException extends RpcException{
    public InvokeTimeoutException(String message) {
        super(message);
    }

    public InvokeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
