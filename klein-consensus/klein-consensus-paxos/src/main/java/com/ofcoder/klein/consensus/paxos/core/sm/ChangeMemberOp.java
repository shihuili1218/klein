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
package com.ofcoder.klein.consensus.paxos.core.sm;

import java.util.Objects;
import java.util.Set;

import com.ofcoder.klein.consensus.facade.sm.SystemOp;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * system operator for change member.
 *
 * @author 释慧利
 */
public class ChangeMemberOp implements SystemOp {
    private String nodeId;
    private Set<Endpoint> newConfig;

    public String getNodeId() {
        return nodeId;
    }

    public Set<Endpoint> getNewConfig() {
        return newConfig;
    }

    public void setNewConfig(final Set<Endpoint> newConfig) {
        this.newConfig = newConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChangeMemberOp that = (ChangeMemberOp) o;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(newConfig, that.newConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, newConfig);
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

}
