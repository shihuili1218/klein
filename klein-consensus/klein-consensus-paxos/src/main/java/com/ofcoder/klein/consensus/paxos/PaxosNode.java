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

import java.util.Objects;

/**
 * @author: 释慧利
 */
public class PaxosNode extends Node {
    private long nextIndex;
    private long nextProposalNo;
    private long lastConfirmProposalNo;
    private String id;

    public long getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }

    public long getNextProposalNo() {
        return nextProposalNo;
    }

    public void setNextProposalNo(long nextProposalNo) {
        this.nextProposalNo = nextProposalNo;
    }

    public long getLastConfirmProposalNo() {
        return lastConfirmProposalNo;
    }

    public void setLastConfirmProposalNo(long lastConfirmProposalNo) {
        this.lastConfirmProposalNo = lastConfirmProposalNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaxosNode paxosNode = (PaxosNode) o;
        return nextIndex == paxosNode.nextIndex && nextProposalNo == paxosNode.nextProposalNo && lastConfirmProposalNo == paxosNode.lastConfirmProposalNo && Objects.equals(id, paxosNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextIndex, nextProposalNo, lastConfirmProposalNo, id);
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "nextIndex=" + nextIndex +
                ", nextProposalNo=" + nextProposalNo +
                ", lastConfirmProposalNo=" + lastConfirmProposalNo +
                ", id='" + id + '\'' +
                "} " + super.toString();
    }

    public static final class Builder {
        private long nextIndex;
        private long nextProposalNo;
        private long lastConfirmProposalNo;
        private String id;

        private Builder() {
        }

        public static Builder aPaxosNode() {
            return new Builder();
        }

        public Builder nextIndex(long nextIndex) {
            this.nextIndex = nextIndex;
            return this;
        }

        public Builder nextProposalNo(long nextProposalNo) {
            this.nextProposalNo = nextProposalNo;
            return this;
        }

        public Builder lastConfirmProposalNo(long lastConfirmProposalNo) {
            this.lastConfirmProposalNo = lastConfirmProposalNo;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public PaxosNode build() {
            PaxosNode paxosNode = new PaxosNode();
            paxosNode.setNextIndex(nextIndex);
            paxosNode.setNextProposalNo(nextProposalNo);
            paxosNode.setLastConfirmProposalNo(lastConfirmProposalNo);
            paxosNode.setId(id);
            return paxosNode;
        }
    }
}
