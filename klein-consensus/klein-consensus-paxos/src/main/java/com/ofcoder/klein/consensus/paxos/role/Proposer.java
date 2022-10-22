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
package com.ofcoder.klein.consensus.paxos.role;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.storage.facade.Instance;

/**
 * @author far.liu
 */
public class Proposer implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Proposer.class);
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
        LOG.info("开始协商，{}", self.getSelf().getId());

        if (!skipPrepare.get()) {
            prepare();
        }

        return null;
    }

    public void accept(ByteBuffer data) {

    }

    public void prepare() {
        self.setCurProposalNo(self.getCurProposalNo() + 1);

        PaxosQuorum quorum = PaxosQuorum.createInstance();
        AtomicBoolean nexted = new AtomicBoolean(false);

        PrepareReq req = PrepareReq.Builder.aPrepareReq()
                .instanceId(self.getNextInstanceId())
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .build();


        // fixme exclude self
        MemberManager.getAllMembers().forEach(it -> {
            InvokeParam param = InvokeParam.Builder.anInvokeParam()
                    .service(PrepareReq.class.getSimpleName())
                    .method(RpcProcessor.KLEIN)
                    .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();
            client.sendRequestAsync(it, param, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(Throwable err) {
                    LOG.error(err.getMessage(),err);
                    quorum.refuse(it);
                    if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                            && nexted.compareAndSet(false, true)) {
                        prepare();
                    }
                }

                @Override
                public void complete(PrepareRes result) {
                    handlePrepareRequest(result, quorum, it, nexted);
                }
            }, prepareTimeout);
        });
    }

    private void handlePrepareRequest(PrepareRes result, PaxosQuorum quorum, Endpoint it, AtomicBoolean nexted) {
        LOG.info("处理Prepare响应，{}", self.getSelf().getId());
        if (result.getResult()) {
            quorum.grant(it);
            if (quorum.isGranted() == Quorum.GrantResult.PASS
                    && nexted.compareAndSet(false, true)) {

//                accept(result.getGrantValue() == null ? );
            }
        } else {
            quorum.refuse(it);
            if (result.getState() == Instance.State.CONFIRMED) {
                // return and learn
            } else if (result.getState() == Instance.State.ACCEPTED) {
                if (result.getProposalNo() > quorum.getMaxRefuseProposalNo()) {
                    quorum.setTempValue(result.getProposalNo(), result.getGrantValue());
                }
                // return and prepare
            }
            self.setCurProposalNo(Math.max(self.getCurProposalNo(), result.getProposalNo()));

            if (quorum.isGranted() == Quorum.GrantResult.REFUSE
                    && nexted.compareAndSet(false, true)) {
                prepare();
            }
        }
    }
}
