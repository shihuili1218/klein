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

import com.ofcoder.klein.consensus.facade.Node;
import com.ofcoder.klein.rpc.facade.Endpoint;

import java.util.Objects;

/**
 * @author: 释慧利
 */
public class PaxosNode extends Node {
    private long nextInstanceId;
    private long curProposalNo;
    private Endpoint self;

    public long getNextInstanceId() {
        return nextInstanceId;
    }

    public void setNextInstanceId(long nextInstanceId) {
        this.nextInstanceId = nextInstanceId;
    }

    public long getCurProposalNo() {
        return curProposalNo;
    }

    public void setCurProposalNo(long curProposalNo) {
        this.curProposalNo = curProposalNo;
    }

    public Endpoint getSelf() {
        return self;
    }

    public void setSelf(Endpoint self) {
        this.self = self;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaxosNode paxosNode = (PaxosNode) o;
        return getNextInstanceId() == paxosNode.getNextInstanceId() && getCurProposalNo() == paxosNode.getCurProposalNo() && Objects.equals(getSelf(), paxosNode.getSelf());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNextInstanceId(), getCurProposalNo(), getSelf());
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "nextInstanceId=" + nextInstanceId +
                ", nextProposalNo=" + curProposalNo +
                ", self=" + self +
                "} " + super.toString();
    }

    public static final class Builder {
        private long nextInstanceId;
        private long nextProposalNo;
        private Endpoint self;

        private Builder() {
        }

        public static Builder aPaxosNode() {
            return new Builder();
        }

        public Builder nextInstanceId(long nextInstanceId) {
            this.nextInstanceId = nextInstanceId;
            return this;
        }

        public Builder nextProposalNo(long nextProposalNo) {
            this.nextProposalNo = nextProposalNo;
            return this;
        }

        public Builder self(Endpoint self) {
            this.self = self;
            return this;
        }

        public PaxosNode build() {
            PaxosNode paxosNode = new PaxosNode();
            paxosNode.setNextInstanceId(nextInstanceId);
            paxosNode.setCurProposalNo(nextProposalNo);
            paxosNode.setSelf(self);
            return paxosNode;
        }
    }
}
