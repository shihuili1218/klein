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
package com.ofcoder.klein.rpc.facade;

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.SPI;

/**
 * grpc client for send request.
 *
 * @author 释慧利
 */
@SPI
public interface RpcClient extends Lifecycle<RpcProp> {

    /**
     * create connection.
     *
     * @param endpoint target
     */
    void createConnection(Endpoint endpoint);

    /**
     * check connection.
     *
     * @param endpoint target
     * @return is connected
     */
    boolean checkConnection(Endpoint endpoint);

    /**
     * close connection.
     *
     * @param endpoint target
     */
    void closeConnection(Endpoint endpoint);

    /**
     * close all connection.
     */
    void closeAll();

    /**
     * send request for async.
     *
     * @param target    target
     * @param request   invoke data and service info
     * @param callback  invoke callback
     * @param timeoutMs invoke timeout
     */
    default void sendRequestAsync(Endpoint target, Serializable request, InvokeCallback callback, long timeoutMs) {
        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(request.getClass().getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(request))).build();
        sendRequestAsync(target, param, callback, timeoutMs);
    }

    /**
     * send request for async.
     *
     * @param target    target
     * @param request   invoke data and service info
     * @param callback  invoke callback
     * @param timeoutMs invoke timeout
     */
    void sendRequestAsync(Endpoint target, InvokeParam request, InvokeCallback callback, long timeoutMs);

    /**
     * send request for sync.
     *
     * @param target    target
     * @param request   invoke data and service info
     * @param timeoutMs invoke timeout
     * @return invoke result
     */
    Object sendRequestSync(Endpoint target, InvokeParam request, long timeoutMs);

}
