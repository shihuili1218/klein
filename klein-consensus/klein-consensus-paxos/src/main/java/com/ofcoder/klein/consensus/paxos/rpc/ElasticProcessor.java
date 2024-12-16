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

import com.google.common.collect.Lists;
import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.consensus.facade.AbstractRpcProcessor;
import com.ofcoder.klein.consensus.facade.Cluster;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ElasticReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ElasticRes;
import com.ofcoder.klein.rpc.facade.RpcContext;

/**
 * Elastic Processor.
 *
 * @author 释慧利
 */
public class ElasticProcessor extends AbstractRpcProcessor<ElasticReq> {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticProcessor.class);
    private final PaxosNode self;
    private final Cluster cluster;

    public ElasticProcessor(final PaxosNode self, final Cluster cluster) {
        this.self = self;
        this.cluster = cluster;
    }

    @Override
    public String service() {
        return ElasticReq.class.getSimpleName();
    }

    @Override
    public void handleRequest(final ElasticReq request, final RpcContext context) {

        switch (request.getOp()) {
            case ElasticReq.LAUNCH:
                if (!MemberRegistry.getInstance().getMemberConfiguration().isValid(request.getEndpoint().getId())) {
                    cluster.changeMember(Lists.newArrayList(request.getEndpoint()), Lists.newArrayList());
                }
                // else: i'm already in the member list.
                break;
            case ElasticReq.SHUTDOWN:
                if (MemberRegistry.getInstance().getMemberConfiguration().isValid(request.getEndpoint().getId())) {
                    cluster.changeMember(Lists.newArrayList(), Lists.newArrayList(request.getEndpoint()));
                }
                // else: i'm not in the member list.
                break;
            default:
                LOG.warn("Unknown operation: {}", request);
                break;
        }

        ElasticRes res = new ElasticRes();
        res.setResult(true);
        context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));

    }

}
