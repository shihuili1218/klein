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

import java.io.Serializable;
import java.util.Objects;

/**
 * Proposal.
 *
 * @author 释慧利
 */
public class Proposal implements Serializable {
    public static final Proposal NOOP = new Proposal(Noop.GROUP, Noop.DEFAULT);
    private String group;
    private Object data;

    public Proposal() {
    }

    public Proposal(final String group, final Object data) {
        this.group = group;
        this.data = data;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public Object getData() {
        return data;
    }

    public void setData(final Object data) {
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
        return Objects.equals(getGroup(), proposal.getGroup()) && Objects.equals(getData(), proposal.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGroup(), getData());
    }

    @Override
    public String toString() {
        return "Proposal{"
                + "group='" + group + '\''
                + ", data=" + data
                + '}';
    }

    /**
     * No operation proposal.
     */
    public static class Noop implements Serializable {
        public static final Noop DEFAULT = new Noop();
        public static final String GROUP = "NOOP";
    }
}
