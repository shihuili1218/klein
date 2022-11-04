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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.core.Acceptor;
import com.ofcoder.klein.consensus.paxos.core.Learner;
import com.ofcoder.klein.consensus.paxos.core.Master;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.Proposer;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.MateData;
import com.ofcoder.klein.storage.facade.Member;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosConsensus.class);
    private PaxosNode self;
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;
    private Master master;
    private ConsensusProp prop;

    private <E extends Serializable> void proposeAsync(final String group, final E data, final ProposeDone done) {
        proposer.propose(group, data, done);
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
            public void applyDone(Object result) {
                builder.data((D) result);
                completed.countDown();
            }
        });
        try {
            if (!completed.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS)) {
                LOG.warn("******** negotiation timeout, data: {}. ********", data);
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
        learner.loadSM(group, sm);
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        loadNode();
        RoleAccessor.create(prop, self);
        this.proposer = RoleAccessor.getProposer();
        this.acceptor = RoleAccessor.getAcceptor();
        this.learner = RoleAccessor.getLearner();
        this.master = RoleAccessor.getMaster();
        loadSM(MasterSM.GROUP, new MasterSM(self.getMemberConfiguration()));

        registerProcessor();

    }

    private void loadNode() {
        // reload self information from storage.
        LogManager<Proposal> logManager = StorageEngine.<Proposal>getInstance().getLogManager();
        MateData mateData = logManager.getMateData();
        PaxosMemberConfiguration configuration;
        if (CollectionUtils.isNotEmpty(mateData.getMembers())) {
            configuration = new PaxosMemberConfiguration();
            configuration.writeOn(prop.getMembers(), this.prop.getSelf());
        } else {
            configuration = new PaxosMemberConfiguration();
            configuration.writeOn(
                    mateData.getMembers().stream().map(it -> new Endpoint(it.getId(), it.getIp(), it.getPort())).collect(Collectors.toList())
                    , this.prop.getSelf()
            );
        }

        this.self = PaxosNode.Builder.aPaxosNode()
                .self(prop.getSelf())
                .curInstanceId(mateData.getMaxInstanceId())
                .curProposalNo(mateData.getMaxProposalNo())
                .memberConfiguration(configuration)
                .build();
        LOG.info("load node: {}", self);
    }


    private void registerProcessor() {
        RpcEngine.registerProcessor(new PrepareProcessor(this.self));
        RpcEngine.registerProcessor(new AcceptProcessor(this.self));
        RpcEngine.registerProcessor(new ConfirmProcessor(this.self));
        RpcEngine.registerProcessor(new LearnProcessor(this.self));
    }


    @Override
    public void shutdown() {
        LogManager<Proposal> logManager = StorageEngine.<Proposal>getInstance().getLogManager();
        logManager.updateConfiguration(self.getMemberConfiguration().getAllMembers().stream().map(
                it -> {
                    Member member = new Member();
                    member.setId(it.getId());
                    member.setIp(it.getIp());
                    member.setPort(it.getPort());
                    return member;
                }
        ).collect(Collectors.toList()), self.getMemberConfiguration().getVersion());
        if (proposer != null) {
            proposer.shutdown();
        }
        if (acceptor != null) {
            acceptor.shutdown();
        }
        if (learner != null) {
            learner.shutdown();
        }
    }
}
