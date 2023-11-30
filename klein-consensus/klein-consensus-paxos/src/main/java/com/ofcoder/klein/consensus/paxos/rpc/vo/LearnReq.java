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

/**
 * learn request data.
 *
 * @author 释慧利
 */
public class LearnReq implements Serializable {
    private String nodeId;
    private long instanceId;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(final long instanceId) {
        this.instanceId = instanceId;
    }

    public static final class Builder {
        private String nodeId;
        private long instanceId;

        private Builder() {
        }

        /**
         * aLearnReq.
         *
         * @return Builder
         */
        public static Builder aLearnReq() {
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
         * instanceId.
         *
         * @param instanceId instanceId
         * @return Builder
         */
        public Builder instanceId(final long instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        /**
         * build.
         *
         * @return LearnReq
         */
        public LearnReq build() {
            LearnReq learnReq = new LearnReq();
            learnReq.instanceId = this.instanceId;
            learnReq.nodeId = this.nodeId;
            return learnReq;
        }
    }
}
