package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * message processor
 *
 * @author: 释慧利
 */
public interface RpcProcessor {

    default String service() {
        return this.getClass().getName();
    }

    String method();

    void handleRequest(final ByteBuffer request, final RpcContext context);

}
