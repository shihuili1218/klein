package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * @author 释慧利
 */
public interface InvokeCallback {
    void error(final Throwable err);
    void complete(final ByteBuffer result);
}
