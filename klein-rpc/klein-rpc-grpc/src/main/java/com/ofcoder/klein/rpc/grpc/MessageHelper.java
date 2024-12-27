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

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistryLite;
import com.ofcoder.klein.rpc.facade.exception.RpcException;
import io.grpc.MethodDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message Helper.
 *
 * @author far.liu
 */
public class MessageHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MessageHelper.class);
    private static final Map<String, MethodDescriptor<DynamicMessage, DynamicMessage>> METHOD_DESCRIPTOR_CACHE = Maps.newConcurrentMap();

    /**
     * build MarshallerDescriptor.
     *
     * @return MarshallerDescriptor
     */
    public static Descriptors.Descriptor buildMarshallerDescriptor() {
        // build Descriptor Proto
        DescriptorProtos.DescriptorProto.Builder jsonMarshaller = DescriptorProtos.DescriptorProto.newBuilder();
        jsonMarshaller.setName(GrpcConstants.JSON_DESCRIPTOR_PROTO_NAME);
        jsonMarshaller.addFieldBuilder()
            .setName(GrpcConstants.JSON_DESCRIPTOR_PROTO_FIELD_NAME)
            .setNumber(1)
            .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES);

        // build File Descriptor Proto
        DescriptorProtos.FileDescriptorProto.Builder fileDescriptorProtoBuilder = DescriptorProtos.FileDescriptorProto.newBuilder();
        fileDescriptorProtoBuilder.addMessageType(jsonMarshaller);

        DescriptorProtos.FileDescriptorProto fileDescriptorProto = fileDescriptorProtoBuilder.build();
        try {
            Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor
                .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0]);
            return fileDescriptor.findMessageTypeByName(GrpcConstants.JSON_DESCRIPTOR_PROTO_NAME);
        } catch (Descriptors.DescriptorValidationException e) {
            LOG.error("dynamic build JsonMarshaller descriptor is fail: {}", e.getMessage());
            throw new RpcException("dynamic build JsonMarshaller descriptor is fail", e);
        }
    }

    /**
     * Build message for json.
     *
     * @param request request data
     * @return DynamicMessage
     */
    public static DynamicMessage buildMessage(final byte[] request) {
        Descriptors.Descriptor jsonDescriptor = buildMarshallerDescriptor();
        DynamicMessage.Builder jsonDynamicMessage = DynamicMessage.newBuilder(jsonDescriptor);
        jsonDynamicMessage.setField(jsonDescriptor.findFieldByName(GrpcConstants.JSON_DESCRIPTOR_PROTO_FIELD_NAME), request);
        return jsonDynamicMessage.build();
    }

    /**
     * Build message for json.
     *
     * @return DynamicMessage
     */
    public static DynamicMessage buildMessage() {
        Descriptors.Descriptor jsonDescriptor = buildMarshallerDescriptor();
        DynamicMessage.Builder jsonDynamicMessage = DynamicMessage.newBuilder(jsonDescriptor);
        return jsonDynamicMessage.build();
    }

    /**
     * get data from DynamicMessage.
     *
     * @param message message
     * @return data
     */
    public static byte[] getDataFromDynamicMessage(final DynamicMessage message) {
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            Object value = entry.getValue();

            String fullName = key.getFullName();
            String jsonMessageFullName = GrpcConstants.JSON_DESCRIPTOR_PROTO_NAME + "." + GrpcConstants.JSON_DESCRIPTOR_PROTO_FIELD_NAME;
            if (jsonMessageFullName.equals(fullName)) {
                return ((ByteString) value).toByteArray();
            }
        }
        return new byte[0];
    }

    /**
     * create MarshallerMethodDescriptor.
     *
     * @param serviceName service name
     * @param methodName  method
     * @param methodType  method type
     * @param request     request dara
     * @param response    response data
     * @return MethodDescriptor
     */
    public static MethodDescriptor<DynamicMessage, DynamicMessage> createMarshallerMethodDescriptor(final String serviceName,
                                                                                                    final String methodName,
                                                                                                    final MethodDescriptor.MethodType methodType,
                                                                                                    final DynamicMessage request,
                                                                                                    final DynamicMessage response) {
        MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor = METHOD_DESCRIPTOR_CACHE.get(serviceName + methodName);
        if (methodDescriptor == null) {
            methodDescriptor = MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                .setType(methodType)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
                .setRequestMarshaller(new DynamicMessageMarshaller(request.getDescriptorForType()))
                .setResponseMarshaller(new DynamicMessageMarshaller(response.getDescriptorForType()))
                .build();
            METHOD_DESCRIPTOR_CACHE.put(serviceName + methodName, methodDescriptor);

        }
        return methodDescriptor;
    }

    private static final class DynamicMessageMarshaller implements MethodDescriptor.Marshaller<DynamicMessage> {

        private final Descriptors.Descriptor messageDescriptor;

        private DynamicMessageMarshaller(final Descriptors.Descriptor messageDescriptor) {
            this.messageDescriptor = messageDescriptor;
        }

        @Override
        public DynamicMessage parse(final InputStream inputStream) {
            try {
                return DynamicMessage.newBuilder(messageDescriptor)
                    .mergeFrom(inputStream, ExtensionRegistryLite.getEmptyRegistry())
                    .build();
            } catch (IOException e) {
                throw new RuntimeException("Unable to merge from the supplied input stream", e);
            }
        }

        @Override
        public InputStream stream(final DynamicMessage abstractMessage) {
            return abstractMessage.toByteString().newInput();
        }
    }

}
