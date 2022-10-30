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
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;

/**
 * @author 释慧利
 */
public class ProposeContext {
    /**
     * The instance that stores data
     */
    private final long instanceId;
    /**
     * Origin data and callback
     */
    private final List<Proposer.ProposeWithDone> dataWithCallback;
    /**
     * The data on which consensus was reached
     */
    private Object consensusData;
    /**
     * Current retry times
     */
    private int times = 0;
    private final Quorum prepareQuorum = PaxosQuorum.createInstance();
    private final AtomicBoolean prepareNexted = new AtomicBoolean(false);
    private final Quorum acceptQuorum = PaxosQuorum.createInstance();
    private final AtomicBoolean acceptNexted = new AtomicBoolean(false);

    public ProposeContext(long instanceId, List<Proposer.ProposeWithDone> events) {
        this.instanceId = instanceId;
        this.dataWithCallback = ImmutableList.copyOf(events);
    }

    public int getTimesAndIncrement() {
        return times++;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public List<Proposer.ProposeWithDone> getDataWithCallback() {
        return dataWithCallback;
    }

    public Object getConsensusData() {
        return consensusData;
    }

    public void setConsensusData(Object consensusData) {
        this.consensusData = consensusData;
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

    /**
     * Creating a new reference
     * Keep news of the last round's lateness from clouding this round's decision-making
     *
     * @return new object for {@link com.ofcoder.klein.consensus.paxos.core.ProposeContext}
     */
    public ProposeContext createRef() {
        ProposeContext target = new ProposeContext(this.instanceId, this.dataWithCallback);
        target.times = this.times;
        target.consensusData = null;
        return target;
    }
}
