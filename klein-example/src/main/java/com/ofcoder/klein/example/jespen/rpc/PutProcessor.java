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
package com.ofcoder.klein.example.jespen.rpc;

import java.nio.ByteBuffer;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.rpc.facade.RpcContext;

/**
 * @author 释慧利
 */
public class PutProcessor extends AbstractRpcProcessor<PutReq> {
    private KleinCache cache;

    public PutProcessor(KleinCache cache) {
        this.cache = cache;
    }

    @Override
    public void handleRequest(PutReq request, RpcContext context) {
        if (request.getTtl() <= 0) {
            cache.put(request.getKey(), request.getData());
        } else {
            cache.put(request.getKey(), request.getData(), request.getTtl(), request.getUnit());
        }
        context.response(ByteBuffer.wrap(Hessian2Util.serialize(true)));
    }

    @Override
    public String service() {
        return PutReq.class.getSimpleName();
    }
}
