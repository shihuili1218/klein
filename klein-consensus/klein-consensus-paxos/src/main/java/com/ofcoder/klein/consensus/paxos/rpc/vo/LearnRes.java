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

import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.storage.facade.Instance;

/**
 * learn response data.
 *
 * @author 释慧利
 */
public class LearnRes implements Serializable {
    private String nodeId;
    private Sync result;
    private Instance<Proposal> instance;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    public Instance<Proposal> getInstance() {
        return instance;
    }

    public void setInstance(final Instance<Proposal> instance) {
        this.instance = instance;
    }

    public Sync isResult() {
        return result;
    }

    public void setResult(final Sync result) {
        this.result = result;
    }

    public static final class Builder {
        private String nodeId;
        private Sync result;
        private Instance<Proposal> instance;

        private Builder() {
        }

        /**
         * aLearnRes.
         *
         * @return Builder
         */
        public static Builder aLearnRes() {
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
        public Builder result(final Sync result) {
            this.result = result;
            return this;
        }

        /**
         * instance.
         *
         * @param instance instance
         * @return Builder
         */
        public Builder instance(final Instance<Proposal> instance) {
            this.instance = instance;
            return this;
        }

        /**
         * build.
         *
         * @return LearnRes
         */
        public LearnRes build() {
            LearnRes learnRes = new LearnRes();
            learnRes.setNodeId(nodeId);
            learnRes.setInstance(instance);
            learnRes.result = this.result;
            return learnRes;
        }
    }
}
