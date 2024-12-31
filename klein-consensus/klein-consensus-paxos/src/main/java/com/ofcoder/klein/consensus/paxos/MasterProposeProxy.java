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
package com.ofcoder.klein.consensus.paxos;

import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.core.MasterState;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.exception.ConnectionException;
import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq.TRANSACTION_REQUEST;

/**
 * Forward the Proposal request to the Master.
 *
 * @author 释慧利
 */
public class MasterProposeProxy implements ProposeProxy {
    private static final Logger LOG = LoggerFactory.getLogger(MasterProposeProxy.class);
    private final RpcClient client;
    private final PaxosNode self;
    private final ConsensusProp prop;
    private final ProposeProxy directProxy;
    private final Serializer proposalValueSerializer;

    public MasterProposeProxy(final ConsensusProp op, final PaxosNode self) {
        this.self = self;
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.directProxy = new UniversalProposeProxy(op);
        this.proposalValueSerializer = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");
    }

    @Override
    public Result propose(final Proposal data, final boolean apply) {
        MasterState masterState = RuntimeAccessor.getMaster().getMaster();
        if (masterState.isSelf() || !prop.getPaxosProp().isEnableMaster()) {
            return this.directProxy.propose(data, apply);
        }

        Result.Builder builder = Result.Builder.aResult();

        Endpoint master = masterState.getMaster();
        if (master == null) {
            LOG.warn("redirect propose request failure, because master is null");
            builder.state(Result.State.FAILURE);
            return builder.build();
        }

        RedirectReq req = RedirectReq.Builder.aRedirectReq()
            .nodeId(this.self.getSelf().getId())
            .redirect(TRANSACTION_REQUEST)
            .proposal(data)
            .apply(apply)
            .build();
        try {
            byte[] content = proposalValueSerializer.serialize(req);
            byte[] response = this.client.sendRequestSync(master, content, this.prop.getRoundTimeout() * this.prop.getRetry() + client.requestTimeout());
            RedirectRes redirectRes = proposalValueSerializer.deserialize(response);
            return redirectRes.getProposeResult();
        } catch (Exception e) {
            if (e instanceof ConnectionException) {
                return builder.state(Result.State.FAILURE).build();
            } else {
                LOG.error("redirect request fail, {}: {}", e.getClass().getName(), e.getMessage());
                return builder.state(Result.State.UNKNOWN).build();
            }
        }
    }

    @Override
    public Long readIndex(final String group) {
        return null;
    }
}
