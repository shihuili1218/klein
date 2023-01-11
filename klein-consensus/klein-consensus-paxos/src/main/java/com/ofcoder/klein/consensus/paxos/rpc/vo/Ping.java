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

/**
 * ping request data.
 *
 * @author 释慧利
 */
public class Ping extends BaseReq {
    private NodeState nodeState;
    private boolean probe;
    private long timestampMs;

    public NodeState getNodeState() {
        return nodeState;
    }

    public boolean isProbe() {
        return probe;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public static final class Builder {
        private String nodeId;
        private long proposalNo;
        private int memberConfigurationVersion;
        private NodeState nodeState;
        private boolean probe;
        private long timestampMs;

        private Builder() {

        }

        /**
         * a ping .
         *
         * @return Builder
         */
        public static Builder aPing() {
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
         * proposalNo.
         *
         * @param proposalNo proposalNo
         * @return Builder
         */
        public Builder proposalNo(final long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        /**
         * memberConfigurationVersion.
         *
         * @param memberConfigurationVersion memberConfigurationVersion
         * @return Builder
         */
        public Builder memberConfigurationVersion(final int memberConfigurationVersion) {
            this.memberConfigurationVersion = memberConfigurationVersion;
            return this;
        }

        /**
         * nodeState.
         *
         * @param nodeState nodeState
         * @return Builder
         */
        public Builder nodeState(final NodeState nodeState) {
            this.nodeState = nodeState;
            return this;
        }

        /**
         * probe.
         *
         * @param probe probe
         * @return Builder
         */
        public Builder probe(final boolean probe) {
            this.probe = probe;
            return this;
        }

        /**
         * timestampMs.
         *
         * @param timestampMs timestampMs
         * @return Builder
         */
        public Builder timestampMs(final long timestampMs) {
            this.timestampMs = timestampMs;
            return this;
        }

        /**
         * build.
         *
         * @return Ping
         */
        public Ping build() {
            Ping ping = new Ping();
            ping.setNodeId(nodeId);
            ping.setProposalNo(proposalNo);
            ping.setMemberConfigurationVersion(memberConfigurationVersion);
            ping.nodeState = this.nodeState;
            ping.probe = this.probe;
            ping.timestampMs = this.timestampMs;
            return ping;
        }
    }
}
