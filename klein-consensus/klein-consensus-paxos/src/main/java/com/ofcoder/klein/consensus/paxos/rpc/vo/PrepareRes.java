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
import java.util.List;

import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.storage.facade.Instance;

/**
 * prepare response data.
 *
 * @author far.liu
 */
public class PrepareRes implements Serializable {
    private String nodeId;
    private boolean result;
    private long curProposalNo;
    private long curInstanceId;
    private List<Instance<Proposal>> instances;

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

    public List<Instance<Proposal>> getInstances() {
        return instances;
    }

    public static final class Builder {
        private String nodeId;
        private boolean result;
        private long curProposalNo;
        private long curInstanceId;
        private List<Instance<Proposal>> instances;

        private Builder() {
        }

        /**
         * aPrepareRes.
         *
         * @return Builder
         */
        public static Builder aPrepareRes() {
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
         * curProposalNo.
         *
         * @param curProposalNo curProposalNo
         * @return Builder
         */
        public Builder curProposalNo(final long curProposalNo) {
            this.curProposalNo = curProposalNo;
            return this;
        }

        /**
         * curInstanceId.
         *
         * @param curInstanceId curInstanceId
         * @return Builder
         */
        public Builder curInstanceId(final long curInstanceId) {
            this.curInstanceId = curInstanceId;
            return this;
        }

        /**
         * instances.
         *
         * @param instances instances
         * @return Builder
         */
        public Builder instances(final List<Instance<Proposal>> instances) {
            this.instances = instances;
            return this;
        }

        /**
         * build.
         *
         * @return PrepareRes
         */
        public PrepareRes build() {
            PrepareRes prepareRes = new PrepareRes();
            prepareRes.result = this.result;
            prepareRes.curInstanceId = this.curInstanceId;
            prepareRes.instances = this.instances;
            prepareRes.curProposalNo = this.curProposalNo;
            prepareRes.nodeId = this.nodeId;
            return prepareRes;
        }
    }
}
