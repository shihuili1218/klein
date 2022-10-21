package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * message processor
 *
 * @author: 释慧利
 */
public interface RpcProcessor {

    String KLEIN = "klein";

    default String service() {
        return KLEIN;
    }

    String method();

    void handleRequest(final ByteBuffer request, final RpcContext context);

}
