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

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.net.SocketAddress;

/**
 * GRPC server interceptor to trace remote address.
 *
 * @author nicholas.jxf
 */
public class RemoteAddressInterceptor implements ServerInterceptor {

    private static final Context.Key<SocketAddress> REMOTE_ADDRESS = Context.key("remote-address");

    @Override
    public <REQT, REST> ServerCall.Listener<REQT> interceptCall(final ServerCall<REQT, REST> call,
                                                                final Metadata headers,
                                                                final ServerCallHandler<REQT, REST> next) {
        final Context ctx = Context.current()
            .withValue(REMOTE_ADDRESS, call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR));
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    public static SocketAddress getRemoteAddress() {
        return REMOTE_ADDRESS.get();
    }
}
