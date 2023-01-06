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
package com.ofcoder.klein.consensus.paxos.rpc;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Nil;
import com.ofcoder.klein.consensus.paxos.rpc.vo.QueryMasterRes;
import com.ofcoder.klein.rpc.facade.RpcContext;

/**
 * Change Member Processor.
 *
 * @author 释慧利
 */
public class QueryMasterProcessor extends AbstractRpcProcessor<Nil> {
    private static final Logger LOG = LoggerFactory.getLogger(QueryMasterProcessor.class);

    private final PaxosNode self;

    public QueryMasterProcessor(final PaxosNode self) {
        this.self = self;
    }

    @Override
    public String service() {
        return Nil.class.getSimpleName();
    }

    @Override
    public void handleRequest(final Nil request, final RpcContext context) {
        QueryMasterRes res = new QueryMasterRes();
        if (self.getMemberConfig().getMaster() != null) {
            res.setMaster(self.getMemberConfig().getMaster());
        }
        context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
    }

}
