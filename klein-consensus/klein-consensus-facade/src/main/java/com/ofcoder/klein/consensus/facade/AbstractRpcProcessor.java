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
package com.ofcoder.klein.consensus.facade;

import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * rpc processor.
 *
 * @author 释慧利
 */
public abstract class AbstractRpcProcessor<R> implements RpcProcessor {
    private Serializer serializer;

    public AbstractRpcProcessor() {
        serializer = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");
    }

    /**
     * handle request.
     *
     * @param request request param
     * @param context rpc context
     */
    public abstract void handleRequest(R request, RpcContext context);

    @Override
    public void handleRequest(final byte[] request, final RpcContext context) {
        R deserialize = (R) serializer.deserialize(request);
        handleRequest(deserialize, context);
    }

    protected void response(final Object msg, final RpcContext context) {
        byte[] serialize = serializer.serialize(msg);
        context.response(serialize);
    }
}
