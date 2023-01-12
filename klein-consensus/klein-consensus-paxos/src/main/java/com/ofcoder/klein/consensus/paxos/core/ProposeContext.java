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
package com.ofcoder.klein.consensus.paxos.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableList;
import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.consensus.facade.quorum.Quorum;
import com.ofcoder.klein.consensus.facade.quorum.QuorumFactory;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;

/**
 * Propose Context.
 *
 * @author 释慧利
 */
public class ProposeContext {
    /**
     * The instance that stores data.
     */
    private final Holder<Long> instanceIdHolder;
    /**
     * Origin data and callback.
     */
    private final List<ProposalWithDone> dataWithCallback;
    /**
     * The data on which consensus was reached.
     */
    private List<Proposal> consensusData;
    /**
     * This is a proposalNo that has executed the prepare phase.
     */
    private long grantedProposalNo;
    /**
     * Current retry times.
     */
    private int times = 0;
    private final PaxosMemberConfiguration memberConfiguration;
    private final Quorum prepareQuorum;
    private final AtomicBoolean prepareNexted;
    private final Quorum acceptQuorum;
    private final AtomicBoolean acceptNexted;

    public ProposeContext(final PaxosMemberConfiguration memberConfiguration, final Holder<Long> instanceIdHolder, final List<ProposalWithDone> events) {
        this.memberConfiguration = memberConfiguration;
        this.instanceIdHolder = instanceIdHolder;
        this.dataWithCallback = ImmutableList.copyOf(events);
        this.prepareQuorum = QuorumFactory.createReadQuorum(memberConfiguration);
        this.prepareNexted = new AtomicBoolean(false);
        this.acceptQuorum = QuorumFactory.createWriteQuorum(memberConfiguration);
        this.acceptNexted = new AtomicBoolean(false);
    }

    public int getTimesAndIncrement() {
        return times++;
    }

    public long getInstanceId() {
        return instanceIdHolder.get();
    }

    public List<ProposalWithDone> getDataWithCallback() {
        return dataWithCallback;
    }

    public List<Proposal> getConsensusData() {
        return consensusData;
    }

    public void setConsensusData(final List<Proposal> consensusData) {
        this.consensusData = consensusData;
    }

    public long getGrantedProposalNo() {
        return grantedProposalNo;
    }

    public void setGrantedProposalNo(final long grantedProposalNo) {
        this.grantedProposalNo = grantedProposalNo;
    }

    public int getTimes() {
        return times;
    }

    public Quorum getPrepareQuorum() {
        return prepareQuorum;
    }

    public AtomicBoolean getPrepareNexted() {
        return prepareNexted;
    }

    public Quorum getAcceptQuorum() {
        return acceptQuorum;
    }

    public AtomicBoolean getAcceptNexted() {
        return acceptNexted;
    }

    public PaxosMemberConfiguration getMemberConfiguration() {
        return memberConfiguration;
    }

    /**
     * Creating a new reference.
     * Keep news of the last round's lateness from clouding this round's decision-making
     *
     * @return new object for {@link com.ofcoder.klein.consensus.paxos.core.ProposeContext}
     */
    public ProposeContext createUntappedRef() {
        ProposeContext target = new ProposeContext(this.memberConfiguration, this.instanceIdHolder, this.dataWithCallback);
        target.times = this.times;
        target.consensusData = null;
        target.grantedProposalNo = 0;
        return target;
    }
}
