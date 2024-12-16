package com.ofcoder.klein.rpc.grpc.ext;

import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

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
    public void handleRequest(ByteBuffer request, RpcContext context) {

        LOG.info("receive client message: {}", (Object) Hessian2Util.deserialize(request.array()));
        context.response(ByteBuffer.wrap(Hessian2Util.serialize("hello, klein")));
    }
}
