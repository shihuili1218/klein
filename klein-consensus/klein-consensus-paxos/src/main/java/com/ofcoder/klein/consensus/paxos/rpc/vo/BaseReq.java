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
 * Base request data.
 * @author 释慧利
 */
public class BaseReq implements Serializable {
    private String nodeId;
    private long proposalNo;
    private int memberConfigurationVersion;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    public long getProposalNo() {
        return proposalNo;
    }

    public void setProposalNo(final long proposalNo) {
        this.proposalNo = proposalNo;
    }

    public int getMemberConfigurationVersion() {
        return memberConfigurationVersion;
    }

    public void setMemberConfigurationVersion(final int memberConfigurationVersion) {
        this.memberConfigurationVersion = memberConfigurationVersion;
    }
}
