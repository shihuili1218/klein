package com.ofcoder.klein.rpc.grpc;

import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.Join;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.MutableHandlerRegistry;

import java.io.IOException;

/**
 * @author: 释慧利
 */
@Join
public class GrpcEngine implements RpcEngine {

    private Server server;

    @Override
    public void init(RpcProp op) {
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
//        handlerRegistry.addService(ServerInterceptors.intercept(serviceDef, this.serverInterceptors.toArray(new ServerInterceptor[0])));
        server = ServerBuilder.forPort(op.getPort())
                .fallbackHandlerRegistry(handlerRegistry)
                .directExecutor()
                .maxInboundMessageSize(op.getMaxInboundMsgSize())
                .build();
        try {
            server.start();
        } catch (IOException e) {
            throw new StartupException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        ServerHelper.shutdownAndAwaitTermination(server, 1000);
    }

}
