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

/**
 * Proposal.
 *
 * @author 释慧利
 */
public class Proposal implements Command {
    private String group;
    private byte[] data;

    public Proposal() {
    }

    public Proposal(final String group, final byte[] data) {
        this.group = group;
        this.data = data;
    }

    @Override
    public boolean ifNoop() {
        return false;
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
        return data;
    }

    public void setData(final byte[] data) {
        this.data = data;
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
        return Objects.equals(group, proposal.group) && Objects.deepEquals(data, proposal.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, Arrays.hashCode(data));
    }
}
