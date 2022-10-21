package com.ofcoder.klein.rpc.facade;

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

    void handleRequest(final String request, final RpcContext context);

}
