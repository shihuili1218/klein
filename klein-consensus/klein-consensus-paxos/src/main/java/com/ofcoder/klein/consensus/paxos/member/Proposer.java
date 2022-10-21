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
package com.ofcoder.klein.consensus.paxos.member;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.RpcProcessor;

/**
 * @author far.liu
 */
public class Proposer implements Lifecycle<ConsensusProp> {
    private AtomicBoolean skipPrepare = new AtomicBoolean(false);
    private RpcClient client;
    private ConsensusProp prop;
    private final PaxosNode self;
    private long prepareTimeout;
    private long acceptTimeout;

    public Proposer(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        this.client = RpcEngine.getClient();
        this.prepareTimeout = (long) (op.getRoundTimeout() * 0.4);
        this.acceptTimeout = op.getRoundTimeout() - prepareTimeout;
    }

    @Override
    public void shutdown() {

    }

    public Result propose(final ByteBuffer data) {
        if (!skipPrepare.get()) {
            prepare();
        }

        return null;
    }

    public void accept() {

    }

    public void prepare() {
        PaxosQuorum quorum = new PaxosQuorum(MemberManager.getMembers());
        AtomicBoolean nexted = new AtomicBoolean(false);

        PrepareReq req = PrepareReq.Builder.aPrepareReq()
                .index(self.getNextIndex())
                .nodeId(self.getId())
                .proposalNo(self.getNextProposalNo())
                .build();
        MemberManager.getMembers().forEach(it -> {
            InvokeParam param = InvokeParam.Builder.anInvokeParam()
                    .service(RpcProcessor.KLEIN)
                    .method(req.getClass().getSimpleName())
                    .data(ByteBuffer.wrap(new byte[0])).build();
            client.sendRequestAsync(it, param, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(Throwable err) {
                    quorum.refuse(it);
                    if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                            && nexted.compareAndSet(false, true)) {
                        prepare();
                    }
                }

                @Override
                public void complete(PrepareRes result) {
                    if (result.getProposalNo() == req.getProposalNo()) {
                        quorum.grant(it);
                        if (quorum.isGranted() == Quorum.GrantResult.PASS
                                && nexted.compareAndSet(false, true)) {
                            accept();
                        }
                    } else {
                        quorum.refuse(it);
                        if (result.getProposalNo() > quorum.getMaxRefuseProposalNo()) {
                            quorum.setTempValue(result.getProposalNo(), result.getGrantValue());
                        }

                        if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                                && nexted.compareAndSet(false, true)) {
                            prepare();
                        }
                    }
                }
            }, prepareTimeout);
        });
    }
}
