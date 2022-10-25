package com.ofcoder.klein.rpc.facade.exception;

import com.ofcoder.klein.common.exception.KleinException;

/**
 * @author 释慧利
 */
public class RpcException extends KleinException {
    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
