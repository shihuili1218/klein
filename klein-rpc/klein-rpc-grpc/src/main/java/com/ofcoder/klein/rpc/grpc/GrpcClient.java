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
import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeCallback;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.rpc.facade.exception.ConnectionException;
import com.ofcoder.klein.rpc.facade.exception.InvokeTimeoutException;
import com.ofcoder.klein.rpc.facade.exception.RpcException;
import com.ofcoder.klein.spi.Join;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Grpc Client.
 *
 * @author 释慧利
 */
@Join
public class GrpcClient implements RpcClient {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcClient.class);
    private final ConcurrentMap<Endpoint, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final RpcProp prop;

    public GrpcClient(final RpcProp op) {
        this.prop = op;
    }

    @Override
    public int requestTimeout() {
        return prop.getRequestTimeout();
    }

    @Override
    public void createConnection(final Endpoint endpoint) {
        if (channels.containsKey(endpoint)) {
            return;
        }
        LOG.info("creating channel: {}", endpoint);
        final ManagedChannel ch = newChannel(endpoint);
        channels.put(endpoint, ch);
    }

    private ManagedChannel newChannel(final Endpoint endpoint) {
        final ManagedChannel ch = ManagedChannelBuilder.forAddress(endpoint.getIp(), endpoint.getPort())
                .usePlaintext()
                .directExecutor()
                .maxInboundMessageSize(prop.getMaxInboundMsgSize())
                .build();
        ch.notifyWhenStateChanged(ConnectivityState.IDLE, () -> onStateChanged(endpoint, ch));
        return ch;
    }

    private void onStateChanged(final Endpoint endpoint, final ManagedChannel channel) {

    }

    @Override
    public boolean checkConnection(final Endpoint endpoint) {
        final ManagedChannel ch = getChannel(endpoint, false);
        if (ch == null) {
            return false;
        }

        final ConnectivityState st = ch.getState(true);
        if (st != ConnectivityState.TRANSIENT_FAILURE && st != ConnectivityState.SHUTDOWN) {
            return true;
        }
        return false;
    }

    @Override
    public void closeConnection(final Endpoint endpoint) {
        if (channels.containsKey(endpoint)) {
            return;
        }
        final ManagedChannel ch = channels.remove(endpoint);
        LOG.info("close channel: {}, {}.", endpoint, ch);
        if (ch != null) {
            ChannelHelper.shutdownAndAwaitTermination(ch);
        }
    }

    @Override
    public void closeAll() {
        if (channels.isEmpty()) {
            return;
        }
        channels.keySet().forEach(this::closeConnection);
    }

    @Override
    public void sendRequestAsync(final Endpoint target, final InvokeParam request, final InvokeCallback callback,
                                 final long timeoutMs) {
        invokeAsync(target, request, callback, timeoutMs);
    }

    @Override
    public <R> R sendRequestSync(final Endpoint target, final InvokeParam request, final long timeoutMs) {
        final CompletableFuture<ByteBuffer> future = new CompletableFuture<>();

        invokeAsync(target, request, new InvokeCallback() {
            @Override
            public void error(final Throwable err) {
                future.completeExceptionally(err);
            }

            @Override
            public void complete(final ByteBuffer result) {
                future.complete(result);
            }
        }, timeoutMs);

        try {
            ByteBuffer result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return Hessian2Util.deserialize(result.array());
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new InvokeTimeoutException(e.getMessage(), e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ConnectionException) {
                throw new ConnectionException(e.getMessage());
            } else {
                throw new RpcException(e.getMessage(), e);
            }
        } catch (final Throwable t) {
            future.cancel(true);
            throw new RpcException(t.getMessage(), t);
        }
    }

    private ManagedChannel getCheckedChannel(final Endpoint endpoint) {
        final ManagedChannel ch = getChannel(endpoint, true);

        if (checkConnection(endpoint)) {
            return ch;
        }
        return null;
    }

    private ManagedChannel getChannel(final Endpoint endpoint, final boolean createIfAbsent) {
        if (createIfAbsent) {
            return this.channels.computeIfAbsent(endpoint, this::newChannel);
        } else {
            return this.channels.get(endpoint);
        }
    }

    /**
     * invoke async.
     *
     * @param endpoint    remote target
     * @param invokeParam request data
     * @param callback    invoke callback
     * @param timeoutMs   invoke timeout
     */
    private void invokeAsync(final Endpoint endpoint, final InvokeParam invokeParam, final InvokeCallback callback, final long timeoutMs) {
        final Channel ch = getCheckedChannel(endpoint);
        if (ch == null) {
            ThreadExecutor.execute(() ->
                    callback.error(new ConnectionException(String.format("connection not available, %s", endpoint))));
            return;
        }

        final DynamicMessage request = MessageHelper.buildMessage(invokeParam.getData());
        final DynamicMessage response = MessageHelper.buildMessage();
        final CallOptions callOpts = CallOptions.DEFAULT.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);
        final MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor = MessageHelper.createMarshallerMethodDescriptor(invokeParam.getService(),
                invokeParam.getMethod(),
                MethodDescriptor.MethodType.UNARY,
                request,
                response);

        ClientCalls.asyncUnaryCall(ch.newCall(methodDescriptor, callOpts), request, new StreamObserver<DynamicMessage>() {

            @Override
            public void onNext(final DynamicMessage value) {
                ByteBuffer respData = MessageHelper.getDataFromDynamicMessage(value);
                ThreadExecutor.execute(() -> callback.complete(respData));
            }

            @Override
            public void onError(final Throwable throwable) {
                ThreadExecutor.execute(() -> callback.error(throwable));
            }

            @Override
            public void onCompleted() {
                // do nothing
            }
        });
    }

    @Override
    public void shutdown() {
        closeAll();
    }
}
