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
package com.ofcoder.klein.consensus.paxos.rpc.vo;

import java.io.Serializable;

import com.ofcoder.klein.storage.facade.Instance;

/**
 * Accept response data.
 *
 * @author far.liu
 */
public class AcceptRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long curProposalNo;
    private long curInstanceId;
    private Instance.State instanceState;
    private NodeState nodeState;

    public String getNodeId() {
        return nodeId;
    }

    public boolean getResult() {
        return result;
    }

    public long getCurProposalNo() {
        return curProposalNo;
    }

    public long getCurInstanceId() {
        return curInstanceId;
    }

    public Instance.State getInstanceState() {
        return instanceState;
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long proposalNo;
        private long instanceId;
        private Instance.State instanceState;
        private NodeState nodeState;

        private Builder() {
        }

        /**
         * anAcceptRes.
         *
         * @return Builder
         */
        public static Builder anAcceptRes() {
            return new Builder();
        }

        /**
         * nodeId.
         *
         * @param nodeId nodeId
         * @return Builder
         */
        public Builder nodeId(final String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * result.
         *
         * @param result result
         * @return Builder
         */
        public Builder result(final boolean result) {
            this.result = result;
            return this;
        }

        /**
         * proposalNo.
         *
         * @param proposalNo proposalNo
         * @return Builder
         */
        public Builder curProposalNo(final long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        /**
         * curInstanceId.
         *
         * @param instanceId curInstanceId
         * @return Builder
         */
        public Builder curInstanceId(final long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * instanceState.
         *
         * @param instanceState instanceState
         * @return Builder
         */
        public Builder instanceState(final Instance.State instanceState) {
            this.instanceState = instanceState;
            return this;
        }

        /**
         * selfState.
         *
         * @param selfState selfState
         * @return Builder
         */
        public Builder nodeState(final NodeState selfState) {
            this.nodeState = selfState;
            return this;
        }

        /**
         * build.
         *
         * @return AcceptRes
         */
        public AcceptRes build() {
            AcceptRes acceptRes = new AcceptRes();
            acceptRes.curInstanceId = this.instanceId;
            acceptRes.result = this.result;
            acceptRes.curProposalNo = this.proposalNo;
            acceptRes.instanceState = this.instanceState;
            acceptRes.nodeId = this.nodeId;
            acceptRes.nodeState = this.nodeState;
            return acceptRes;
        }
    }
}
