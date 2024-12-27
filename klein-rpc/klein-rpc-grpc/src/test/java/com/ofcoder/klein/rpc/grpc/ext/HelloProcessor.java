package com.ofcoder.klein.rpc.grpc.ext;

import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 释慧利
 */
public class HelloProcessor implements RpcProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HelloProcessor.class);

    @Override
    public String service() {
        return String.class.getSimpleName();
    }

    @Override
    public void handleRequest(byte[] request, RpcContext context) {

        LOG.info("receive client message: {}", new String(request, StandardCharsets.UTF_8));
        context.response("hello, klein".getBytes(StandardCharsets.UTF_8));
    }
}
