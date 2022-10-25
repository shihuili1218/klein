package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * @author 释慧利
 */
public interface RpcContext {
    void response(ByteBuffer msg);
    String getRemoteAddress();
}
