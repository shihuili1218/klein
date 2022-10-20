package com.ofcoder.klein.rpc.facade;

/**
 * @author: 释慧利
 */
public interface InvokeCallback {
    void complete(final Object result, final Throwable err);
}
