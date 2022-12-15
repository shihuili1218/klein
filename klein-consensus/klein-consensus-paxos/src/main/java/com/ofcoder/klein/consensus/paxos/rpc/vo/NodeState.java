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
 * @author 释慧利
 */
public class NodeState implements Serializable {
    private String nodeId;
    private long maxInstanceId;
    private long lastAppliedInstanceId;
    private long lastCheckpoint;

    public String getNodeId() {
        return nodeId;
    }

    public long getMaxInstanceId() {
        return maxInstanceId;
    }

    public long getLastAppliedInstanceId() {
        return lastAppliedInstanceId;
    }

    public long getLastCheckpoint() {
        return lastCheckpoint;
    }


    public static final class Builder {
        private String nodeId;
        private long maxInstanceId;
        private long lastAppliedInstanceId;
        private long lastCheckpoint;

        private Builder() {
        }

        public static Builder aNodeState() {
            return new Builder();
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder maxInstanceId(long maxInstanceId) {
            this.maxInstanceId = maxInstanceId;
            return this;
        }

        public Builder lastAppliedInstanceId(long lastAppliedInstanceId) {
            this.lastAppliedInstanceId = lastAppliedInstanceId;
            return this;
        }

        public Builder lastCheckpoint(long lastCheckpoint) {
            this.lastCheckpoint = lastCheckpoint;
            return this;
        }

        public NodeState build() {
            NodeState nodeState = new NodeState();
            nodeState.nodeId = this.nodeId;
            nodeState.lastCheckpoint = this.lastCheckpoint;
            nodeState.lastAppliedInstanceId = this.lastAppliedInstanceId;
            nodeState.maxInstanceId = this.maxInstanceId;
            return nodeState;
        }
    }
}
