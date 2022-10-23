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
package com.ofcoder.klein.consensus.facade;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author: 释慧利
 */
public abstract class Quorum {
    private Set<Endpoint> allMembers = new HashSet<>();
    private Set<Endpoint> grantedMembers = new HashSet<>();
    private Set<Endpoint> failedMembers = new HashSet<>();
    private long maxRefuseProposalNo = 0;
    private List<ByteBuffer> tempValue = null;
    private int threshold;

    public Quorum(final Set<Endpoint> allMembers) {
        this.allMembers = allMembers;
        this.threshold = allMembers.size() / 2 + 1;
    }

    public GrantResult isGranted() {
        if (grantedMembers.size() >= threshold) {
            return GrantResult.PASS;
        } else if (failedMembers.size() >= threshold) {
            return GrantResult.REFUSE;
        } else {
            return GrantResult.GRANTING;
        }
    }

    public boolean grant(Endpoint node) {
        if (!allMembers.contains(node)) {
            return false;
        }
        return grantedMembers.add(node);
    }

    public boolean refuse(Endpoint node) {
        if (!allMembers.contains(node)) {
            return false;
        }
        return failedMembers.add(node);
    }

    public void setTempValue(long maxRefuseProposalNo, List<ByteBuffer> tempValue){
        this.maxRefuseProposalNo = maxRefuseProposalNo;
        this.tempValue = tempValue;
    }

    public long getMaxRefuseProposalNo() {
        return maxRefuseProposalNo;
    }

    public List<ByteBuffer> getTempValue() {
        return tempValue;
    }

    public static enum GrantResult {
        PASS,
        REFUSE,
        GRANTING
    }
}
