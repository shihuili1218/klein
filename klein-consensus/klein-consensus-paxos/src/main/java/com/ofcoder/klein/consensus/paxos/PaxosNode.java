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
    private AtomicLong curProposalNo;
    private Endpoint self;

    public long incrementProposalNo() {
        return addProposalNo(1);
    }

    public long addProposalNo(long v) {
        return curProposalNo.addAndGet(v);
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
        return curProposalNo.get();
    }

    public Endpoint getSelf() {
        return self;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaxosNode paxosNode = (PaxosNode) o;
        return getCurInstanceId() == paxosNode.getCurInstanceId() && getCurProposalNo() == paxosNode.getCurProposalNo() && Objects.equals(getSelf(), paxosNode.getSelf());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCurInstanceId(), getCurProposalNo(), getSelf());
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "nextInstanceId=" + curInstanceId +
                ", nextProposalNo=" + curProposalNo +
                ", self=" + self +
                "} " + super.toString();
    }

    public static final class Builder {
        private AtomicLong curInstanceId;
        private AtomicLong curProposalNo;
        private Endpoint self;

        private Builder() {
        }

        public static Builder aPaxosNode() {
            return new Builder();
        }

        public Builder curInstanceId(AtomicLong nextInstanceId) {
            this.curInstanceId = nextInstanceId;
            return this;
        }

        public Builder curProposalNo(AtomicLong nextProposalNo) {
            this.curProposalNo = nextProposalNo;
            return this;
        }

        public Builder self(Endpoint self) {
            this.self = self;
            return this;
        }

        public PaxosNode build() {
            PaxosNode paxosNode = new PaxosNode();
            paxosNode.curInstanceId = curInstanceId;
            paxosNode.curProposalNo = curProposalNo;
            paxosNode.self = self;
            return paxosNode;
        }
    }
}
