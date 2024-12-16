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
package com.ofcoder.klein.jepsen.server.rpc;

import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.rpc.facade.RpcContext;

import java.nio.ByteBuffer;

/**
 * cache exists request processor.
 *
 * @author 释慧利
 */
public class ExistsProcessor extends AbstractRpcProcessor<ExistsReq> {
    private KleinCache cache;

    public ExistsProcessor(final KleinCache cache) {
        this.cache = cache;
    }

    @Override
    public void handleRequest(final ExistsReq request, final RpcContext context) {
        context.response(ByteBuffer.wrap(Hessian2Util.serialize(cache.exist(request.getKey()))));
    }

    @Override
    public String service() {
        return ExistsReq.class.getSimpleName();
    }
}
