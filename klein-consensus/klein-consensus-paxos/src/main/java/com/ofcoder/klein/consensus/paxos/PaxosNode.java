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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.ofcoder.klein.consensus.facade.Node;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class PaxosNode extends Node {
    private AtomicLong curInstanceId;
    private long curProposalNo;
    private Endpoint self;
    private PaxosMemberConfiguration memberConfiguration;

    /**
     * nextProposalNo = N * J + I
     * N: Total number of members, J: local self increasing integer, I: member id
     *
     * @return next proposalNo
     */
    public long generateNextProposalNo() {
        long cur = this.curProposalNo;
        int n = memberConfiguration.getAllMembers().size();
        long j = cur / n;
        if (cur % n > 0){
            j = j + 1;
        }
        return n * j + Integer.parseInt(self.getId());
    }

    public void setCurProposalNo(long proposalNo) {
        curProposalNo = Math.max(curProposalNo, proposalNo);
    }

    public long incrementInstanceId() {
        return addInstanceId(1);
    }

    public long addInstanceId(long v) {
        return curInstanceId.addAndGet(v);
    }


    public long getCurInstanceId() {
        return curInstanceId.get();
    }

    public long getCurProposalNo() {
        return curProposalNo;
    }

    public Endpoint getSelf() {
        return self;
    }

    public PaxosMemberConfiguration getMemberConfiguration() {
        return memberConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaxosNode paxosNode = (PaxosNode) o;
        return Objects.equals(getCurInstanceId(), paxosNode.getCurInstanceId()) && Objects.equals(getCurProposalNo(), paxosNode.getCurProposalNo()) && Objects.equals(getSelf(), paxosNode.getSelf()) && Objects.equals(memberConfiguration, paxosNode.memberConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCurInstanceId(), getCurProposalNo(), getSelf(), memberConfiguration);
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "curInstanceId=" + curInstanceId +
                ", curProposalNo=" + curProposalNo +
                ", self=" + self +
                ", memberConfiguration=" + memberConfiguration +
                "} " + super.toString();
    }

    public static final class Builder {
        private AtomicLong curInstanceId;
        private long curProposalNo;
        private Endpoint self;
        private PaxosMemberConfiguration memberConfiguration;

        private Builder() {
        }

        public static Builder aPaxosNode() {
            return new Builder();
        }

        public Builder curInstanceId(AtomicLong curInstanceId) {
            this.curInstanceId = curInstanceId;
            return this;
        }

        public Builder curProposalNo(long curProposalNo) {
            this.curProposalNo = curProposalNo;
            return this;
        }

        public Builder self(Endpoint self) {
            this.self = self;
            return this;
        }

        public Builder memberConfiguration(PaxosMemberConfiguration memberConfiguration) {
            this.memberConfiguration = memberConfiguration;
            return this;
        }

        public PaxosNode build() {
            PaxosNode paxosNode = new PaxosNode();
            paxosNode.curProposalNo = this.curProposalNo;
            paxosNode.curInstanceId = this.curInstanceId;
            paxosNode.self = this.self;
            paxosNode.memberConfiguration = this.memberConfiguration;
            return paxosNode;
        }
    }
}
