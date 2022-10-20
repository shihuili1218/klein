package com.ofcoder.klein.rpc.grpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeCallback;
import com.ofcoder.klein.rpc.facade.Transmitter;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.rpc.facade.exception.ConnectionException;
import com.ofcoder.klein.rpc.facade.exception.InvokeTimeoutException;
import com.ofcoder.klein.rpc.facade.exception.RpcException;
import com.ofcoder.klein.rpc.grpc.proto.ProtobufParser;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author: 释慧利
 */
public class GrpcTransmitter implements Transmitter {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcTransmitter.class);
    private final ConcurrentMap<Endpoint, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final RpcProp prop;

    public GrpcTransmitter(final RpcProp prop) {
        this.prop = prop;
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
    public void closeAll(Endpoint endpoint) {
        if (channels.isEmpty()) {
            return;
        }
        channels.keySet().forEach(this::checkConnection);
    }

    @Override
    public void sendRequest(Endpoint target, Object request, InvokeCallback callback, long timeoutMs) {
        invokeAsync(target, request, callback, timeoutMs);
    }

    @Override
    public Object sendRequestSync(Endpoint target, Object request, long timeoutMs) {
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

    public void invokeAsync(final Endpoint endpoint, final Object request, final InvokeCallback callback, final long timeoutMs) {
        Requires.requireNonNull(endpoint, "endpoint");
        Requires.requireNonNull(request, "request");

        final Channel ch = getCheckedChannel(endpoint);
        if (ch == null) {
            ThreadExecutor.submit(() -> {
                callback.complete(null, new ConnectionException(String.format("connection not available, %s", endpoint)));
            });
            return;
        }

        final MethodDescriptor<Message, Message> method = getCallMethod(request);
        final CallOptions callOpts = CallOptions.DEFAULT.withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS);

        // fixme switch for MethodDescriptor.MethodType
        ClientCalls.asyncUnaryCall(ch.newCall(method, callOpts), (Message) request, new StreamObserver<Message>() {

            @Override
            public void onNext(final Message value) {
                ThreadExecutor.submit(() -> callback.complete(value, null));
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



    private DynamicMessage getCallMethod2(final String request) {
        Descriptors.Descriptor jsonDescriptor = buildJsonMarshallerDescriptor();
        DynamicMessage.Builder jsonDynamicMessage = DynamicMessage.newBuilder(jsonDescriptor);
        jsonDynamicMessage.setField(jsonDescriptor.findFieldByName("data"), request);
        return jsonDynamicMessage.build();
    }

    private Descriptors.Descriptor buildJsonMarshallerDescriptor() {
        // build Descriptor Proto
        DescriptorProtos.DescriptorProto.Builder jsonMarshaller = DescriptorProtos.DescriptorProto.newBuilder();
        jsonMarshaller.setName("JsonMessage");
        jsonMarshaller.addFieldBuilder()
                .setName("data")
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);

        // build File Descriptor Proto
        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder();
        fileDescriptorProtoBuilder.addMessageType(jsonMarshaller);

        DescriptorProtos.FileDescriptorProto fileDescriptorProto = fileDescriptorProtoBuilder.build();
        try {
            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor
                    .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
            return fileDescriptor.findMessageTypeByName("JsonMessage");
        } catch (Descriptors.DescriptorValidationException e) {
            LOG.error("dynamic build JsonMarshaller descriptor is fail: {}", e.getMessage());
            throw new RuntimeException("dynamic build JsonMarshaller descriptor is fail", e);
        }
    }
    private MethodDescriptor<Message, Message> getCallMethod(final Object request) {
        final String interest = request.getClass().getName();
        ProtobufParser.Whole whole = ProtobufParser.get(interest);

        return MethodDescriptor //
                .<Message, Message>newBuilder() //
                .setType(MethodDescriptor.MethodType.UNARY) //
                .setFullMethodName(MethodDescriptor.generateFullMethodName(interest, ProtobufParser.FIXED_METHOD_NAME)) //
                .setRequestMarshaller(ProtoUtils.marshaller(whole.getReq())) //
                .setResponseMarshaller(ProtoUtils.marshaller(whole.getRes())) //
                .build();
    }

}
