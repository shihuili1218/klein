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

import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepare Request Processor.
 *
 * @author 释慧利
 */
public class PrepareProcessor implements RpcProcessor<PrepareReq, PrepareRes> {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareProcessor.class);

    public PrepareProcessor(final PaxosNode self) {
        // do nothing.
    }

    @Override
    public String service() {
        return PrepareReq.class.getSimpleName();
    }

    @Override
    public PrepareRes handleRequest(final PrepareReq request) {

        if (!MemberRegistry.getInstance().getMemberConfiguration().isValid(request.getNodeId())) {
            LOG.error("msg type: prepare, from nodeId[{}] not in my membership(or i'm null membership), skip this message. ",
                    request.getNodeId());
            return null;
        }

        return RuntimeAccessor.getAcceptor().handlePrepareRequest(request, false);
    }
}
