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
 * SnapSync request data.
 *
 * @author 释慧利
 */
public class SnapSyncReq extends BaseReq {

    private long checkpoint;

    public long getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(final long checkpoint) {
        this.checkpoint = checkpoint;
    }

    public static final class Builder {
        private String nodeId;
        private long proposalNo;
        private int memberConfigurationVersion;
        private long checkpoint;

        private Builder() {
        }

        /**
         * aSnapSyncReq.
         *
         * @return Builder
         */
        public static Builder aSnapSyncReq() {
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
         * checkpoint.
         *
         * @param checkpoint checkpoint
         * @return Builder
         */
        public Builder checkpoint(final long checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

        /**
         * build.
         *
         * @return SnapSyncReq
         */
        public SnapSyncReq build() {
            SnapSyncReq snapSyncReq = new SnapSyncReq();
            snapSyncReq.setNodeId(nodeId);
            snapSyncReq.setProposalNo(proposalNo);
            snapSyncReq.setMemberConfigurationVersion(memberConfigurationVersion);
            snapSyncReq.checkpoint = this.checkpoint;
            return snapSyncReq;
        }
    }
}
