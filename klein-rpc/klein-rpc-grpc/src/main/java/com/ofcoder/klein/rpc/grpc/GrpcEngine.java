package com.ofcoder.klein.rpc.grpc;

import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.Join;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.util.MutableHandlerRegistry;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        shutdownAndAwaitTermination();
    }

    private void shutdownAndAwaitTermination() {
        if (server == null) {
            return;
        }
        // disable new tasks from being submitted
        server.shutdown();
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        long timeoutMillis = 1000;
        final long phaseOne = timeoutMillis / 5;
        try {
            // wait a while for existing tasks to terminate
            if (server.awaitTermination(phaseOne, unit)) {
                return;
            }
            server.shutdownNow();
            // wait a while for tasks to respond to being cancelled
            if (server.awaitTermination(timeoutMillis - phaseOne, unit)) {
                return;
            }
            LOG.warn("Fail to shutdown grpc server: {}.", server);
        } catch (final InterruptedException e) {
            // (Re-)cancel if current thread also interrupted
            server.shutdownNow();
            // preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
