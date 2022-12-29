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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * MemberConfiguration.
 *
 * @author far.liu
 */
public class MemberConfiguration implements Serializable, LogManager.ClusterConfig {
    private static final Logger LOG = LoggerFactory.getLogger(MemberConfiguration.class);
    protected AtomicInteger version = new AtomicInteger(0);
    protected volatile Map<String, Endpoint> allMembers = new ConcurrentHashMap<>();

    public int getVersion() {
        return version.get();
    }

    /**
     * get all members.
     *
     * @return all members
     */
    public Set<Endpoint> getAllMembers() {
        return new HashSet<>(allMembers.values());
    }

    public Set<Endpoint> getMembersWithout(final String selfId) {
        return getAllMembers().stream().filter(it -> !StringUtils.equals(selfId, it.getId()))
                .collect(Collectors.toSet());
    }

    /**
     * check node is valid.
     *
     * @param nodeId check node id
     * @return is valid
     */
    public boolean isValid(final String nodeId) {
        return allMembers.containsKey(nodeId);
    }

    /**
     * get endpoint info by node id.
     *
     * @param id node id
     * @return endpoint
     */
    public Endpoint getEndpointById(final String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return allMembers.getOrDefault(id, null);
    }

    /**
     * add member.
     *
     * @param node new member
     */
    protected void writeOn(final Endpoint node) {
        allMembers.put(node.getId(), node);
        version.incrementAndGet();
    }

    /**
     * remove member.
     *
     * @param node error member
     */
    protected void writeOff(final Endpoint node) {
        allMembers.remove(node.getId());
        version.incrementAndGet();
    }

    /**
     * init configuration.
     *
     * @param nodes all members
     */
    public void init(final List<Endpoint> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        this.allMembers.putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.version.incrementAndGet();
    }

    @Override
    public String toString() {
        return "MemberConfiguration{"
                + "version=" + version
                + ", allMembers=" + allMembers
                + '}';
    }
}
