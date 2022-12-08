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
 * @author 释慧利
 */
public class SnapSyncReq extends BaseReq {

    private long checkpoint;

    public long getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(long checkpoint) {
        this.checkpoint = checkpoint;
    }

    public static final class Builder {
        private String nodeId;
        private long proposalNo;
        private int memberConfigurationVersion;
        private long checkpoint;

        private Builder() {
        }

        public static Builder aSnapSyncReq() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder proposalNo(long proposalNo) {
            this.proposalNo = proposalNo;
            return this;
        }

        public Builder memberConfigurationVersion(int memberConfigurationVersion) {
            this.memberConfigurationVersion = memberConfigurationVersion;
            return this;
        }

        public Builder checkpoint(long checkpoint) {
            this.checkpoint = checkpoint;
            return this;
        }

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
