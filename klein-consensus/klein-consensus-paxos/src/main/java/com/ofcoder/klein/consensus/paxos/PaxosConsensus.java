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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.HeartbeatProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.SnapSyncProcessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosConsensus.class);
    private PaxosNode self;
    private ConsensusProp prop;

    private <E extends Serializable> void proposeAsync(final String group, final E data, final ProposeDone done) {
        RoleAccessor.getProposer().propose(group, data, done);
    }

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply) {
        int count = apply ? 2 : 1;

        CountDownLatch completed = new CountDownLatch(count);
        Result.Builder<D> builder = Result.Builder.aResult();
        proposeAsync(group, data, new ProposeDone() {
            @Override
            public void negotiationDone(Result.State result) {
                builder.state(result);
                completed.countDown();
                if (result == Result.State.UNKNOWN) {
                    completed.countDown();
                }
            }

            @Override
            public void applyDone(Object input, Object result) {
                if (data.equals(input)){
                    builder.data((D) result);
                }
                completed.countDown();
            }
        });
        try {
            if (!completed.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS)) {
                LOG.warn("******** negotiation timeout ********");
                builder.state(Result.State.UNKNOWN);
            }
        } catch (InterruptedException e) {
            throw new ConsensusException(e.getMessage(), e);
        }
        return builder.build();
    }

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> read(final String group, E data) {
        return propose(group, data, true);
    }

    @Override
    public void loadSM(final String group, SM sm) {
        RoleAccessor.getLearner().loadSM(group, sm);
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        loadNode();
        registerProcessor();
        RoleAccessor.create(prop, self);
        loadSM(MasterSM.GROUP, new MasterSM(self.getMemberConfiguration()));
        RoleAccessor.getMaster().electingMaster();

        preheating();
    }

    private void preheating() {
//        propose(Proposal.Noop.GROUP, Proposal.Noop.DEFAULT, true);
    }

    private void loadNode() {
        // reload self information from storage.

        LogManager<Proposal, PaxosNode> logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        PaxosMemberConfiguration configuration = new PaxosMemberConfiguration();
        configuration.init(this.prop.getMembers(), this.prop.getSelf());

        this.self = logManager.loadMateData(PaxosNode.Builder.aPaxosNode()
                .curInstanceId(0)
                .curAppliedInstanceId(0)
                .curProposalNo(0)
                .lastCheckpoint(0)
                .self(prop.getSelf())
                .memberConfiguration(configuration)
                .build());

        LOG.info("self info: {}", self);
    }


    private void registerProcessor() {
        RpcEngine.registerProcessor(new PrepareProcessor(this.self));
        RpcEngine.registerProcessor(new AcceptProcessor(this.self));
        RpcEngine.registerProcessor(new ConfirmProcessor(this.self));
        RpcEngine.registerProcessor(new LearnProcessor(this.self));
        RpcEngine.registerProcessor(new HeartbeatProcessor(this.self));
        RpcEngine.registerProcessor(new SnapSyncProcessor(this.self));
    }


    @Override
    public void shutdown() {
        if (RoleAccessor.getProposer() != null) {
            RoleAccessor.getProposer().shutdown();
        }
        if (RoleAccessor.getAcceptor() != null) {
            RoleAccessor.getAcceptor().shutdown();
        }
        if (RoleAccessor.getLearner() != null) {
            RoleAccessor.getLearner().shutdown();
        }
        if (RoleAccessor.getMaster() != null) {
            RoleAccessor.getMaster().shutdown();
        }
    }
}
