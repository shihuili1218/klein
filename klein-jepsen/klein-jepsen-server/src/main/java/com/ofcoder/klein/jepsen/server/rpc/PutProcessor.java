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

import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.rpc.facade.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cache put request processor.
 *
 * @author 释慧利
 */
public class PutProcessor extends AbstractRpcProcessor<PutReq> {
    private static final Logger LOG = LoggerFactory.getLogger(PutProcessor.class);
    private KleinCache cache;

    public PutProcessor(final KleinCache cache) {
        this.cache = cache;
    }

    @Override
    public void handleRequest(final PutReq request, final RpcContext rpcContext) {
        try {
            LOG.info("put operator, begin, seq: {}", request.getSeq());
            Result.State put = cache.put(request.getKey(), request.getData(), false, request.getTtl(), request.getUnit());
            LOG.info("put operator, end, seq: {}, result: {}", request.getSeq(), put);
            response(put.name(), rpcContext);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            LOG.info("put operator, err, seq: {}, result: err", request.getSeq());
            response(Result.State.UNKNOWN.name(), rpcContext);
        }
    }

    @Override
    public String service() {
        return PutReq.class.getSimpleName();
    }
}
