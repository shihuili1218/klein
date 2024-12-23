package com.ofcoder.klein.rpc.grpc.ext;

import com.ofcoder.klein.rpc.facade.RpcProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 释慧利
 */
public class HelloProcessor implements RpcProcessor<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(HelloProcessor.class);

    @Override
    public String service() {
        return String.class.getSimpleName();
    }

    @Override
    public String handleRequest(String request) {

        LOG.info("receive client message: {}", request);
        return "hello, klein";
    }
}
