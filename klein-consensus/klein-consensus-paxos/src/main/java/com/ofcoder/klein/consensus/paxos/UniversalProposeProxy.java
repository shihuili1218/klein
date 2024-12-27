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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;

/**
 * Call Proposer directly to initiate a proposal.
 *
 * @author 释慧利
 */
public class UniversalProposeProxy implements ProposeProxy {
    private static final Logger LOG = LoggerFactory.getLogger(UniversalProposeProxy.class);
    private final ConsensusProp prop;

    public UniversalProposeProxy(final ConsensusProp prop) {
        this.prop = prop;
    }

    @Override
    public Result propose(final Proposal proposal, final boolean apply) {
        CountDownLatch completed = new CountDownLatch(1);
        Result.Builder builder = Result.Builder.aResult();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Direct Propose, outsider: {}, write on master: {}", prop.getSelf().isOutsider(), prop.getPaxosProp().isEnableMaster());
        }
        RuntimeAccessor.getProposer().propose(proposal, new ProposeDone() {
            @Override
            public void negotiationDone(final boolean result, final boolean changed) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Direct Propose negotiationDone, result: {}, change: {}", result, changed);
                }
                if (result) {
                    builder.state(!changed ? Result.State.SUCCESS : Result.State.FAILURE);
                } else {
                    builder.state(Result.State.UNKNOWN);
                    if (!apply) {
                        completed.countDown();
                    }
                }
            }

            @Override
            public void applyDone(final Map<Command, byte[]> result) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Direct Propose applyDone, result: {}", result);
                }
                builder.data(result.get(proposal));
                completed.countDown();
            }
        }, false);

        try {
            if (!completed.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS)) {
                LOG.warn("******** negotiation timeout {}, {} ********", completed, proposal);
                builder.state(Result.State.UNKNOWN);
            }
        } catch (InterruptedException e) {
            throw new ConsensusException(e.getMessage(), e);
        }
        return builder.build();
    }

    @Override
    public Long readIndex(final String group) {
        // only for enabled master
        return null;
    }

}
