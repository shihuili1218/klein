package com.ofcoder.klein.rpc.facade;

import java.nio.ByteBuffer;

/**
 * message processor
 *
 * @author: 释慧利
 */
public interface RpcProcessor {

    String KLEIN = "klein";

    String service();

    default String method() {
        return KLEIN;
    }

    void handleRequest(final ByteBuffer request, final RpcContext context);

}
