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

import com.ofcoder.klein.consensus.facade.Node;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberManager;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class PaxosNode extends Node {
    private long curInstanceId = 0;
    private final Object instanceIdLock = new Object();
    private long curAppliedInstanceId = 0;
    private final Object appliedInstanceIdLock = new Object();
    private long curProposalNo = 0;
    private final Object proposalNoLock = new Object();
    private long lastCheckpoint = 0;
    private final Object checkpointLock = new Object();
    private Endpoint self;

    /**
     * nextProposalNo = N * J + I
     * N: Total number of members, J: local self increasing integer, I: member id
     *
     * @return next proposalNo
     */
    public long generateNextProposalNo() {
        long cur = this.curProposalNo;
        int n = MemberManager.getAllMembers().size();
        long j = cur / n;
        if (cur % n > 0) {
            j = j + 1;
        }
        long next = n * j + Integer.parseInt(self.getId());
        updateCurProposalNo(next);
        return this.curProposalNo;
    }


    /**
     * This is a synchronous method, Double-Check-Lock
     *
     * @param proposalNo target value
     */
    public void updateCurProposalNo(long proposalNo) {
        if (curProposalNo < proposalNo) {
            synchronized (proposalNoLock) {
                this.curProposalNo = Math.max(curProposalNo, proposalNo);
            }
        }
    }

    public long getCurProposalNo() {
        return curProposalNo;
    }

    public long incrementInstanceId() {
        updateCurInstanceId(curInstanceId + 1);
        return this.curInstanceId;
    }

    public void updateCurInstanceId(long instanceId) {
        if (curInstanceId < instanceId) {
            synchronized (instanceIdLock) {
                this.curInstanceId = Math.max(curInstanceId, instanceId);
            }
        }
    }

    public long getCurInstanceId() {
        return curInstanceId;
    }

    public Endpoint getSelf() {
        return self;
    }

    public long getCurAppliedInstanceId() {
        return curAppliedInstanceId;
    }

    public void updateCurAppliedInstanceId(long appliedInstanceId) {
        if (this.curAppliedInstanceId < appliedInstanceId) {
            synchronized (appliedInstanceIdLock) {
                this.curAppliedInstanceId = Math.max(curAppliedInstanceId, appliedInstanceId);
            }
        }
    }

    public long getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void updateLastCheckpoint(long checkpoint) {
        if (lastCheckpoint < checkpoint) {
            synchronized (checkpointLock) {
                this.lastCheckpoint = Math.max(lastCheckpoint, checkpoint);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaxosNode node = (PaxosNode) o;
        return getCurInstanceId() == node.getCurInstanceId() && getCurAppliedInstanceId() == node.getCurAppliedInstanceId() && getCurProposalNo() == node.getCurProposalNo() && lastCheckpoint == node.lastCheckpoint && Objects.equals(getSelf(), node.getSelf());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCurInstanceId(), getCurAppliedInstanceId(), getCurProposalNo(), lastCheckpoint, getSelf());
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "curInstanceId=" + curInstanceId +
                ", curAppliedInstanceId=" + curAppliedInstanceId +
                ", curProposalNo=" + curProposalNo +
                ", lastCheckpoint=" + lastCheckpoint +
                ", self=" + self +
                "} " + super.toString();
    }

    public String nodeId() {
        return self.getId();
    }

    public static final class Builder {
        private long curInstanceId;
        private long curAppliedInstanceId;
        private long curProposalNo;
        private long lastCheckpoint;
        private Endpoint self;

        private Builder() {
        }

        public static Builder aPaxosNode() {
            return new Builder();
        }

        public Builder curInstanceId(long curInstanceId) {
            this.curInstanceId = curInstanceId;
            return this;
        }

        public Builder curAppliedInstanceId(long curAppliedInstanceId) {
            this.curAppliedInstanceId = curAppliedInstanceId;
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

        public Builder lastCheckpoint(long lastCheckpoint) {
            this.lastCheckpoint = lastCheckpoint;
            return this;
        }

        public PaxosNode build() {
            PaxosNode paxosNode = new PaxosNode();
            paxosNode.curProposalNo = this.curProposalNo;
            paxosNode.curInstanceId = this.curInstanceId;
            paxosNode.curAppliedInstanceId = this.curAppliedInstanceId;
            paxosNode.self = this.self;
            paxosNode.lastCheckpoint = this.lastCheckpoint;
            return paxosNode;
        }
    }
}
