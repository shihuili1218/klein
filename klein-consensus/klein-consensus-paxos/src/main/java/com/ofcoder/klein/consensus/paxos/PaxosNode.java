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
package com.ofcoder.klein.consensus.paxos;

import com.ofcoder.klein.consensus.facade.Node;

import java.util.Objects;

/**
 * @author: 释慧利
 */
public class PaxosNode extends Node {
    private long nextProposalNo;
    private long lastConfirmProposalNo;
    private String id;

    public long getNextProposalNo() {
        return nextProposalNo;
    }

    public void setNextProposalNo(long nextProposalNo) {
        this.nextProposalNo = nextProposalNo;
    }

    public long getLastConfirmProposalNo() {
        return lastConfirmProposalNo;
    }

    public void setLastConfirmProposalNo(long lastConfirmProposalNo) {
        this.lastConfirmProposalNo = lastConfirmProposalNo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaxosNode paxosNode = (PaxosNode) o;
        return nextProposalNo == paxosNode.nextProposalNo && lastConfirmProposalNo == paxosNode.lastConfirmProposalNo && Objects.equals(id, paxosNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nextProposalNo, lastConfirmProposalNo, id);
    }

    @Override
    public String toString() {
        return "PaxosNode{" +
                "nextProposalNo=" + nextProposalNo +
                ", lastConfirmProposalNo=" + lastConfirmProposalNo +
                ", id='" + id + '\'' +
                '}';
    }
}
