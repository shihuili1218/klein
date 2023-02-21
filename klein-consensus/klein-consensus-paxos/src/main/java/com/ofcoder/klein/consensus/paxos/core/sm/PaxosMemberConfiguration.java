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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * paxos member configuration.
 *
 * @author 释慧利
 */
public class PaxosMemberConfiguration extends MemberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosMemberConfiguration.class);
    private volatile Endpoint master;
    private final Object masterLock = new Object();
    private volatile Endpoint candidate;

    public Endpoint getMaster() {
        return this.candidate != null ? this.candidate : this.master;
    }

    /**
     * cache a candidate.
     *
     * @param nodeId candidate
     */
    public void seenCandidate(final String nodeId) {
        if (isValid(nodeId)) {
            this.candidate = getEndpointById(nodeId);
        }
    }

    /**
     * change master.
     *
     * @param nodeId new master id
     * @return change result
     */
    public boolean changeMaster(final String nodeId) {
        if (isValid(nodeId)) {
            synchronized (masterLock) {
                this.master = getEndpointById(nodeId);
                this.version.incrementAndGet();
            }
            this.candidate = null;
            RoleAccessor.getMaster().onChangeMaster(nodeId);
            LOG.info("node-{} was promoted to master, version: {}", nodeId, this.version.get());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void init(final List<Endpoint> nodes) {
        super.init(nodes);
    }

    /**
     * load snapshot.
     *
     * @param snap snapshot
     */
    protected void loadSnap(final PaxosMemberConfiguration snap) {
        synchronized (masterLock) {
            this.master = null;
            if (snap.master != null) {
                this.master = new Endpoint(snap.master.getId(), snap.master.getIp(), snap.master.getPort());
            }
            this.version = new AtomicInteger(snap.version.get());
        }
        this.effectMembers.clear();
        this.effectMembers.putAll(snap.effectMembers);
        this.lastMembers.clear();
        this.lastMembers.putAll(snap.lastMembers);
    }

    /**
     * Create an object with the same data.
     *
     * @return new object
     */
    public PaxosMemberConfiguration createRef() {
        PaxosMemberConfiguration target = new PaxosMemberConfiguration();
        target.effectMembers.putAll(effectMembers);
        target.lastMembers.putAll(lastMembers);
        synchronized (masterLock) {
            if (master != null) {
                target.master = new Endpoint(master.getId(), master.getIp(), master.getPort());
            }
            target.version = new AtomicInteger(version.get());
        }
        return target;
    }

    @Override
    public String toString() {
        return "PaxosMemberConfiguration{"
                + "master=" + master
                + ", version=" + version
                + ", effectMembers=" + effectMembers
                + ", lastMembers=" + lastMembers
                + '}';
    }
}
