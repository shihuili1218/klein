package com.ofcoder.klein.rpc.grpc.ext;

import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: 释慧利
 */
public class HelloProcessor implements RpcProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HelloProcessor.class);

    @Override
    public String method() {
        return "hello";
    }

    @Override
    public void handleRequest(String request, RpcContext context) {
        LOG.info("receive client message: {}", request);
        context.response("hello, klein");
    }
}
