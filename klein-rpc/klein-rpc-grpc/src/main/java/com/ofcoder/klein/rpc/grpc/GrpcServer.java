/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.rpc.grpc;

import com.google.protobuf.DynamicMessage;
import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.RpcServer;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Grpc Server.
 *
 * @author far.liu
 */
@Join
public class GrpcServer implements RpcServer {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcServer.class);
    private final Server server;
    private final Serializer rpcSerializer;
    private final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();

    public GrpcServer(final RpcProp op) {
        server = ServerBuilder.forPort(op.getPort())
                .fallbackHandlerRegistry(handlerRegistry)
                .directExecutor()
                .maxInboundMessageSize(op.getMaxInboundMsgSize())
                .build();

        rpcSerializer = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");
        try {
            server.start();
        } catch (IOException e) {
            throw new StartupException(e.getMessage(), e);
        }
    }

    @Override
    public void registerProcessor(final RpcProcessor processor) {
        final MethodDescriptor<DynamicMessage, DynamicMessage> method = MessageHelper.createMarshallerMethodDescriptor(
                processor.service(),
                processor.method(),
                MethodDescriptor.MethodType.UNARY,
                MessageHelper.buildMessage(),
                MessageHelper.buildMessage(ByteBuffer.wrap(new byte[0])));

        final ServerCallHandler<DynamicMessage, DynamicMessage> handler =
                ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                    ByteBuffer msg = MessageHelper.getDataFromDynamicMessage(request);
                    Object deserialize = rpcSerializer.deserialize(msg.array());

                    Object response = processor.handleRequest(deserialize);
                    if (response != null) {
                        DynamicMessage res = MessageHelper.buildMessage(ByteBuffer.wrap(rpcSerializer.serialize(response)));
                        responseObserver.onNext(res);
                        responseObserver.onCompleted();
                    }

                });

        final ServerServiceDefinition serviceDef = ServerServiceDefinition
                .builder(processor.service())
                .addMethod(method, handler)
                .build();
        this.handlerRegistry.addService(ServerInterceptors.intercept(serviceDef, new RemoteAddressInterceptor()));
    }

    @Override
    public void shutdown() {
        ServerHelper.shutdownAndAwaitTermination(server, 1000);
    }

    @Override
    public Serializer getSerializer() {
        return rpcSerializer;
    }
}
