package com.ofcoder.klein.rpc.grpc;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DynamicMessage;
import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.RpcServer;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.Join;

import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;

/**
 * @author far.liu
 */
@Join
public class GrpcServer implements RpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);
    private Server server;
    final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();

    @Override
    public void registerProcessor(RpcProcessor processor) {
        final MethodDescriptor<DynamicMessage, DynamicMessage> method = MessageHelper.createMarshallerMethodDescriptor(
                processor.service(),
                processor.method(),
                MethodDescriptor.MethodType.UNARY,
                MessageHelper.buildMessage(),
                MessageHelper.buildMessage(ByteBuffer.wrap(new byte[0])));

        final ServerCallHandler<DynamicMessage, DynamicMessage> handler = ServerCalls.asyncUnaryCall(
                (request, responseObserver) -> {
                    final SocketAddress remoteAddress = RemoteAddressInterceptor.getRemoteAddress();
                    ByteBuffer msg = MessageHelper.getDataFromDynamicMessage(request);
                    ThreadExecutor.submit(() -> processor.handleRequest(msg, new RpcContext() {
                        @Override
                        public void response(ByteBuffer msg) {
                            final DynamicMessage res = MessageHelper.buildMessage(msg);
                            responseObserver.onNext(res);
                            responseObserver.onCompleted();
                        }

                        @Override
                        public String getRemoteAddress() {
                            // Rely on GRPC's capabilities, not magic (netty channel)
                            return remoteAddress != null ? remoteAddress.toString() : null;
                        }
                    }));
                });

        final ServerServiceDefinition serviceDef = ServerServiceDefinition //
                .builder(processor.service()) //
                .addMethod(method, handler) //
                .build();
        this.handlerRegistry.addService(ServerInterceptors.intercept(serviceDef, new RemoteAddressInterceptor()));
    }

    @Override
    public void init(final RpcProp op) {
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
