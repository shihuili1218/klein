package com.ofcoder.klein.rpc.grpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DynamicMessage;
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

/**
 * @author: 释慧利
 */
@Join
public class GrpcClient implements RpcClient {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcClient.class);
    private final ConcurrentMap<Endpoint, ManagedChannel> channels = new ConcurrentHashMap<>();
    private RpcProp prop;

    @Override
    public void createConnection(final Endpoint endpoint) {
        if (channels.containsKey(endpoint)) {
            return;
        }
        LOG.info("creating channel: {}", endpoint);
        final ManagedChannel ch = newChannel(endpoint);
        channels.put(endpoint, ch);
    }

    private ManagedChannel newChannel(Endpoint endpoint) {
        final ManagedChannel ch = ManagedChannelBuilder.forAddress(endpoint.getIp(), endpoint.getPort())
                .usePlaintext()
                .directExecutor()
                .maxInboundMessageSize(prop.getMaxInboundMsgSize())
                .build();
        ch.notifyWhenStateChanged(ConnectivityState.IDLE, () -> onStateChanged(endpoint, ch));
        return ch;
    }

    private void onStateChanged(Endpoint endpoint, ManagedChannel channel) {

    }

    @Override
    public boolean checkConnection(Endpoint endpoint) {
        if (!channels.containsKey(endpoint)) {
            return false;
        }
        ManagedChannel managedChannel = channels.get(endpoint);
        //todo
        return true;
    }

    @Override
    public void closeConnection(Endpoint endpoint) {
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
    public void sendRequest(Endpoint target, InvokeParam request, InvokeCallback callback, long timeoutMs) {
        invokeAsync(target, request, callback, timeoutMs);
    }

    @Override
    public Object sendRequestSync(Endpoint target, InvokeParam request, long timeoutMs) {
        final CompletableFuture<Object> future = new CompletableFuture<>();

        invokeAsync(target, request, (result, err) -> {
            if (err == null) {
                future.complete(result);
            } else {
                future.completeExceptionally(err);
            }
        }, timeoutMs);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new InvokeTimeoutException(e.getMessage(), e);
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

    public void invokeAsync(final Endpoint endpoint, final InvokeParam invokeParam, final InvokeCallback callback, final long timeoutMs) {
        final Channel ch = getCheckedChannel(endpoint);
        if (ch == null) {
            ThreadExecutor.submit(() -> {
                callback.complete(null, new ConnectionException(String.format("connection not available, %s", endpoint)));
            });
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
                String respData = MessageHelper.getDataFromDynamicMessage(value);
                ThreadExecutor.submit(() -> callback.complete(respData, null));
            }

            @Override
            public void onError(final Throwable throwable) {
                ThreadExecutor.submit(() -> callback.complete(null, throwable));
            }

            @Override
            public void onCompleted() {
                // do nothing
            }
        });
    }

    @Override
    public void init(final RpcProp op) {
        this.prop = op;
    }

    @Override
    public void shutdown() {
        closeAll();
    }
}
