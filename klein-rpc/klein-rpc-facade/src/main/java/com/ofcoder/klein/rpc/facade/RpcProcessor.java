package com.ofcoder.klein.rpc.facade;

/**
 * message processor
 *
 * @author: 释慧利
 */
public interface RpcProcessor {

    String service();
    String method();

    void handleRequest(final String request);

}
