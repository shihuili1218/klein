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

import java.nio.ByteBuffer;

import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.common.serialization.Hessian2Util;

/**
 * rpc processor.
 *
 * @author 释慧利
 */
public abstract class AbstractRpcProcessor<R> implements RpcProcessor {

    /**
     * handle request.
     *
     * @param request request param
     * @param context rpc context
     */
    public abstract void handleRequest(R request, RpcContext context);

    @Override
    public void handleRequest(final ByteBuffer request, final RpcContext context) {
        R deserialize = Hessian2Util.deserialize(request.array());
        handleRequest(deserialize, context);
    }
}
