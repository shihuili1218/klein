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

import com.ofcoder.klein.consensus.facade.Command;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Proposal.
 *
 * @author 释慧利
 */
public class Proposal implements Command {
    private String group;
    private byte[] data;
    private Boolean ifSystemOp;

    public Proposal() {
    }

    public Proposal(final String group, final byte[] data, final Boolean ifSystemOp) {
        this.group = group;
        this.data = data;
        this.ifSystemOp = ifSystemOp;
    }

    @Override
    public boolean getIfSystemOp() {
        return ifSystemOp;
    }

    public void setIfSystemOp(final Boolean ifSystemOp) {
        this.ifSystemOp = ifSystemOp;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    @Override
    public byte[] getData() {
        return data.clone();
    }

    public void setData(final byte[] data) {
        this.data = data.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Proposal proposal = (Proposal) o;
        return Objects.equals(group, proposal.group)
            && Arrays.equals(data, proposal.data)
            && Objects.equals(ifSystemOp, proposal.ifSystemOp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, Arrays.hashCode(data), ifSystemOp);
    }

    @Override
    public String toString() {
        return "Proposal{"
            + "group='" + group + '\''
            + ", data=" + Optional.ofNullable(data).map(value -> value.length).orElse(0)
            + ", ifSystemOp=" + ifSystemOp
            + '}';
    }
}
