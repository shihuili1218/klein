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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.core.Master;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.HeartbeatProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.NewMasterProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.RedirectProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.SnapSyncProcessor;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * Paxos Consensus.
 *
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosConsensus.class);
    private PaxosNode self;
    private ConsensusProp prop;
    private RpcClient client;

    private void proposeAsync(final Proposal data, final ProposeDone done) {
        RoleAccessor.getProposer().propose(data, done);
    }

    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(final String group, final E data, final boolean apply) {
        int count = apply ? 2 : 1;

        CountDownLatch completed = new CountDownLatch(count);
        Result.Builder<D> builder = Result.Builder.aResult();
        Proposal proposal = new Proposal(group, data);
        proposeAsync(proposal, new ProposeDone() {
            @Override
            public void negotiationDone(final boolean result, final List<Proposal> consensusDatas) {
                completed.countDown();
                if (result) {
                    builder.state(consensusDatas.contains(proposal) ? Result.State.SUCCESS : Result.State.FAILURE);
                } else {
                    builder.state(Result.State.UNKNOWN);
                    completed.countDown();
                }
            }

            @Override
            public void applyDone(final Map<Proposal, Object> applyResults) {
                for (Map.Entry<Proposal, Object> entry : applyResults.entrySet()) {
                    if (entry.getKey() == proposal) {
                        builder.data((D) entry.getValue());
                        break;
                    }
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
    public void loadSM(final String group, final SM sm) {
        RoleAccessor.getLearner().loadSM(group, sm);
    }

    @Override
    public void setListener(final LifecycleListener listener) {
        RoleAccessor.getMaster().addHealthyListener(healthy -> {
            if (Master.ElectState.allowPropose(healthy)) {
                listener.prepared();
            }
        });
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();

        loadNode();

        registerProcessor();
        RoleAccessor.create(prop, self);
        loadSM(MasterSM.GROUP, new MasterSM(self.getMemberConfig()));
        RoleAccessor.getMaster().electingMaster();

        preheating();
    }

    private void preheating() {
//        propose(Proposal.Noop.GROUP, Proposal.Noop.DEFAULT, true);
    }

    private void loadNode() {
        // reload self information from storage.
        PaxosMemberConfiguration defaultValue = new PaxosMemberConfiguration();
        defaultValue.init(prop.getMembers());

        LogManager<Proposal> logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.self = (PaxosNode) logManager.loadMetaData(PaxosNode.Builder.aPaxosNode()
                .curInstanceId(0)
                .curAppliedInstanceId(0)
                .curProposalNo(0)
                .lastCheckpoint(0)
                .self(prop.getSelf())
                .memberConfig(defaultValue)
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
        RpcEngine.registerProcessor(new NewMasterProcessor(this.self));
        RpcEngine.registerProcessor(new RedirectProcessor(this.self));
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

    @Override
    public MemberConfiguration getMemberConfig() {
        return self.getMemberConfig();
    }

    @Override
    public void addMember(final Endpoint endpoint) {
        if (getMemberConfig().isValid(endpoint.getId())) {
            return;
        }
        RoleAccessor.getMaster().changeMember(ADD, Lists.newArrayList(endpoint));
    }

    @Override
    public void removeMember(final Endpoint endpoint) {
        if (!getMemberConfig().isValid(endpoint.getId())) {
            return;
        }
        RoleAccessor.getMaster().changeMember(REMOVE, Lists.newArrayList(endpoint));
    }

}
