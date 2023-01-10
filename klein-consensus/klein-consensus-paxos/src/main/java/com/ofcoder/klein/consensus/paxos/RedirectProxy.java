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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;

import static com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq.TRANSACTION_REQUEST;

/**
 * @author 释慧利
 */
public class RedirectProxy implements Proxy {
    private static final Logger LOG = LoggerFactory.getLogger(RedirectProxy.class);
    private RpcClient client;
    private PaxosNode self;
    private ConsensusProp prop;

    public RedirectProxy(final ConsensusProp op, final PaxosNode self) {
        this.self = self;
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
    }

    @Override
    public void propose(Proposal data, ProposeDone done) {
        if (RoleAccessor.getMaster().isSelf()) {
            RoleAccessor.getProposer().propose(data, done);
            return;
        }
        Endpoint master = MemberRegistry.getInstance().getMemberConfiguration().getMaster();
        if (master == null) {
            LOG.warn("redirect propose request failure, because master is null");
            done.negotiationDone(false, null);
            return;
        }
        RedirectReq req = RedirectReq.Builder.aRedirectReq()
                .nodeId(this.self.getSelf().getId())
                .redirect(TRANSACTION_REQUEST)
                .proposal(data)
                .build();
        this.client.sendRequestAsync(master, req, new AbstractInvokeCallback<RedirectRes>() {
            @Override
            public void error(Throwable err) {
                done.negotiationDone(false, null);
            }

            @Override
            public void complete(RedirectRes result) {

            }
        }, this.prop.getRoundTimeout() * this.prop.getRetry() + 50);

    }
}
