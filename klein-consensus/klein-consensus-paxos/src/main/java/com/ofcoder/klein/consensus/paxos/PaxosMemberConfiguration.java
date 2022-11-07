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

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
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
    public static final String NULL_MASTER = "NULL";
    private volatile Endpoint master;

    public Endpoint getMaster() {
        return master;
    }

    public boolean changeMaster(String nodeId) {
        if (isValid(nodeId) || StringUtils.equals(nodeId, NULL_MASTER)) {
            LOG.info("node-{} was promoted to master", nodeId);
            master = allMembers.getOrDefault(nodeId, null);
            version.incrementAndGet();
            RoleAccessor.getMaster().onChangeMaster(nodeId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public PaxosMemberConfiguration createRef() {
        PaxosMemberConfiguration target = new PaxosMemberConfiguration();
        target.writeOn(new ArrayList<>(this.allMembers.values()), this.self);
        target.master = this.master;
        target.version = this.version;
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
