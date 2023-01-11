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

import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;

/**
 * Call Proposer directly to initiate a proposal.
 *
 * @author 释慧利
 */
public class DirectProxy implements Proxy {
    private static final Logger LOG = LoggerFactory.getLogger(DirectProxy.class);
    private ConsensusProp prop;

    public DirectProxy(final ConsensusProp prop) {
        this.prop = prop;
    }

    @Override
    public <D extends Serializable> Result<D> propose(final Proposal proposal, final boolean apply) {
        int count = apply ? 2 : 1;

        CountDownLatch completed = new CountDownLatch(count);
        Result.Builder<D> builder = Result.Builder.aResult();
        RoleAccessor.getProposer().propose(proposal, new ProposeDone() {
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
}
