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
 * @author 释慧利
 */
public class LearnRes implements Serializable {
    private String nodeId;
    private Instance<Proposal> instance;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instance<Proposal> getInstance() {
        return instance;
    }

    public void setInstance(Instance<Proposal> instance) {
        this.instance = instance;
    }

    public static final class Builder {
        private String nodeId;
        private Instance<Proposal> instance;

        private Builder() {
        }

        public static Builder aLearnRes() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder instance(Instance<Proposal> instance) {
            this.instance = instance;
            return this;
        }

        public LearnRes build() {
            LearnRes learnRes = new LearnRes();
            learnRes.nodeId = this.nodeId;
            learnRes.instance = this.instance;
            return learnRes;
        }
    }
}
