package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * @author: 释慧利
 */
public interface InvokeCallback {
    void complete(final ByteBuffer result, final Throwable err);
}
