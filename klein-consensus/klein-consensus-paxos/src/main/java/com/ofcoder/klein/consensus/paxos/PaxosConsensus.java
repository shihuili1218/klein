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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.core.Acceptor;
import com.ofcoder.klein.consensus.paxos.core.Learner;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.Proposer;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.LearnProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;
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
    private ConsensusProp prop;

    private <E extends Serializable> void proposeAsync(final E data, final ProposeDone done) {
        proposer.propose(data, done);
    }


    @Override
    public <E extends Serializable, D extends Serializable> Result<D> propose(E data, final boolean apply) {
        int count = apply ? 2 : 1;

        CountDownLatch completed = new CountDownLatch(count);
        Result.Builder<D> builder = Result.Builder.aResult();
        proposeAsync(data, new ProposeDone() {
            @Override
            public void negotiationDone(Result.State result) {
                builder.state(result);
                completed.countDown();
                if (result == Result.State.UNKNOWN) {
                    completed.countDown();
                }
            }

            @Override
            public  void applyDone(Object result) {
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
    public <E extends Serializable, D extends Serializable> Result<D> read(E data) {
        return propose(data, true);
    }

    @Override
    public void loadSM(SM sm) {
        learner.loadSM(sm);
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        MemberManager.writeOn(op.getMembers(), this.prop.getSelf());
        loadNode();
        RoleAccessor.create(prop, self);
        this.proposer = RoleAccessor.getProposer();
        this.acceptor = RoleAccessor.getAcceptor();
        this.learner = RoleAccessor.getLearner();
        registerProcessor();
    }

    private void loadNode() {
        // reload self information from storage.
        LogManager logManager = StorageEngine.getLogManager();
        this.self = PaxosNode.Builder.aPaxosNode()
                .self(prop.getSelf())
                .curInstanceId(new AtomicLong(logManager.maxInstanceId()))
                .curProposalNo(new AtomicLong(logManager.maxProposalNo()))
                .build();
        LOG.info("load node: {}", self);
    }

    private void registerProcessor() {
        RpcEngine.registerProcessor(new PrepareProcessor());
        RpcEngine.registerProcessor(new AcceptProcessor());
        RpcEngine.registerProcessor(new ConfirmProcessor());
        RpcEngine.registerProcessor(new LearnProcessor());
    }


    @Override
    public void shutdown() {
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
