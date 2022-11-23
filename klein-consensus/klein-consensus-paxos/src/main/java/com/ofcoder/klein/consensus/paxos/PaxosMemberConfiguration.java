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

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class PaxosMemberConfiguration extends MemberConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosMemberConfiguration.class);
    private volatile Endpoint master;

    public Endpoint getMaster() {
        return master;
    }

    public boolean changeMaster(String nodeId) {
        if (isValid(nodeId)) {
            master = allMembers.get(nodeId);
            version.incrementAndGet();
            RoleAccessor.getMaster().onChangeMaster(nodeId);
            LOG.info("node-{} was promoted to master, version: {}", nodeId, version.get());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public PaxosMemberConfiguration createRef() {
        PaxosMemberConfiguration target = new PaxosMemberConfiguration();
        target.allMembers.putAll(this.allMembers);
        target.self = this.self;
        if (this.master != null) {
            target.master = new Endpoint(this.master.getId(), this.master.getIp(), this.master.getPort());
        }
        target.version = new AtomicInteger(this.version.get());
        return target;
    }

    @Override
    public String toString() {
        return "PaxosMemberConfiguration{" +
                "master=" + master +
                ", version=" + version +
                ", allMembers=" + allMembers +
                ", self=" + self +
                '}';
    }
}
