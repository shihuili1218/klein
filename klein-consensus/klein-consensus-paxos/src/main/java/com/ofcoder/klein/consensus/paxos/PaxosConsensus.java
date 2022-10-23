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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.role.Acceptor;
import com.ofcoder.klein.consensus.paxos.role.Learner;
import com.ofcoder.klein.consensus.paxos.role.ProposeDone;
import com.ofcoder.klein.consensus.paxos.role.Proposer;
import com.ofcoder.klein.consensus.paxos.rpc.AcceptProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.ConfirmProcessor;
import com.ofcoder.klein.consensus.paxos.rpc.PrepareProcessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.exception.InvokeTimeoutException;
import com.ofcoder.klein.spi.Join;

/**
 * @author far.liu
 */
@Join
public class PaxosConsensus implements Consensus {
    private PaxosNode self;
    private Proposer proposer;
    private Acceptor acceptor;
    private Learner learner;
    private ConsensusProp prop;

    @Override
    public Result propose(final ByteBuffer data) {
        final CompletableFuture<Result> future = new CompletableFuture<>();
        proposeAsync(data, future::complete);

        try {
            return future.get(this.prop.getRoundTimeout(), TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            future.cancel(true);
            throw new InvokeTimeoutException(e.getMessage(), e);
        } catch (final Throwable t) {
            future.cancel(true);
            throw new ConsensusException(t.getMessage(), t);
        }
    }

    private void proposeAsync(final ByteBuffer data, final ProposeDone done) {
        proposer.propose(data, done);
    }

    @Override
    public Result read(ByteBuffer data) {
        return null;
    }

    @Override
    public void loadSM(SM sm) {

    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        MemberManager.writeOn(op.getMembers(), this.prop.getSelf());
        loadNode();
        initProposer();
        initAcceptor();
        initLearner();
        registerProcessor();
    }

    private void initLearner() {
        this.learner = new Learner(this.self);
        this.learner.init(prop);
    }

    private void initAcceptor() {
        this.acceptor = new Acceptor(this.self);
        this.acceptor.init(prop);
    }

    private void initProposer() {
        this.proposer = new Proposer(this.self);
        this.proposer.init(prop);
    }

    private void loadNode() {
        // fixme reload from storage.
        this.self = PaxosNode.Builder.aPaxosNode()
                .self(prop.getSelf())
                .curInstanceId(new AtomicLong(0))
                .curProposalNo(new AtomicLong(0))
                .build();
    }

    private void registerProcessor() {
        RpcEngine.registerProcessor(new PrepareProcessor(this.acceptor));
        RpcEngine.registerProcessor(new AcceptProcessor(this.acceptor));
        RpcEngine.registerProcessor(new ConfirmProcessor());
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
