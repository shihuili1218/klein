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

import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author 释慧利
 */
public class ProposeContext {
    private long instanceId;
    private List<Object> datas;
    private List<ProposeDone> dones;
    private int times = 0;
    private Quorum prepareQuorum;
    private AtomicBoolean prepareNexted;
    private Quorum acceptQuorum;
    private AtomicBoolean acceptNexted;

    public ProposeContext(long instanceId, List<Proposer.ProposeWithDone> events) {
        this(instanceId, events.stream().map(Proposer.ProposeWithDone::getData).collect(Collectors.toList())
                , events.stream().map(Proposer.ProposeWithDone::getDone).collect(Collectors.toList()));
    }

    public ProposeContext(long instanceId, List<Object> datas, List<ProposeDone> dones) {
        this.instanceId = instanceId;
        this.datas = datas;
        this.dones = dones;
        reset();
    }

    public int getTimesAndIncrement() {
        return times++;
    }

    public void reset() {
        this.prepareQuorum = PaxosQuorum.createInstance();
        this.prepareNexted = new AtomicBoolean(false);
        this.acceptQuorum = PaxosQuorum.createInstance();
        this.acceptNexted = new AtomicBoolean(false);
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public List<Object> getDatas() {
        return datas;
    }

    public List<ProposeDone> getDones() {
        return dones;
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
}
