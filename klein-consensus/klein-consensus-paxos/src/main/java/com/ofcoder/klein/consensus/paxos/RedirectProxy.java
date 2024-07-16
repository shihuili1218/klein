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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;

import static com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq.TRANSACTION_REQUEST;

/**
 * Forward the Proposal request to the Master.
 *
 * @author 释慧利
 */
public class RedirectProxy implements Proxy {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectProxy.class);
    private final RpcClient client;
    private final PaxosNode self;
    private final ConsensusProp prop;
    private final Proxy directProxy;

    public RedirectProxy(final ConsensusProp op, final PaxosNode self) {
        this.self = self;
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.directProxy = new DirectProxy(op);
    }

    @Override
    public <D extends Serializable> Result<D> propose(final Proposal data, final boolean apply) {
        if (RuntimeAccessor.getMaster().isSelf()) {
            return this.directProxy.propose(data, apply);
        }

        Result.Builder<D> builder = Result.Builder.aResult();

        Endpoint master = MemberRegistry.getInstance().getMemberConfiguration().getMaster();
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
        RedirectRes res = this.client.sendRequestSync(master, req, this.prop.getRoundTimeout() * this.prop.getRetry() + client.requestTimeout());
        if (res == null) {
            return builder.state(Result.State.UNKNOWN).build();
        }
        return (Result<D>) res.getProposeResult();
    }

    @Override
    public Result<Long> readIndex(final String group) {
        return null;
    }
}
