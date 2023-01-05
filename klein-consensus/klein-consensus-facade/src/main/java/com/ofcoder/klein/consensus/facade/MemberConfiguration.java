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
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.exception.ChangeMemberException;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * MemberConfiguration.
 *
 * @author far.liu
 */
public class MemberConfiguration implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(MemberConfiguration.class);
    protected AtomicInteger version = new AtomicInteger(0);
    protected Map<String, Endpoint> effectMembers = new ConcurrentHashMap<>();
    protected transient Map<String, Endpoint> lastMembers = new ConcurrentHashMap<>();

    public int getVersion() {
        return version.get();
    }

    /**
     * Set the last seen configuration.
     *
     * @param newConfig new configuration
     */
    public void seenNewConfig(final Set<Endpoint> newConfig) {
        if (MapUtils.isNotEmpty(lastMembers)) {
            throw new ChangeMemberException("lastMembers is not empty, the config may be changing");
        }
        this.version.incrementAndGet();
        this.lastMembers.putAll(newConfig.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
    }

    /**
     * Commit the last seen configuration.
     *
     * @param newConfig new configuration
     */
    public void effectiveNewConfig(final Set<Endpoint> newConfig) {
        if (MapUtils.isEmpty(lastMembers) || !new HashSet<>(lastMembers.values()).equals(newConfig)) {
            throw new ChangeMemberException("lastMembers is empty, this error should not occur");
        }
        this.effectMembers = lastMembers;
        this.lastMembers = new ConcurrentHashMap<>();
    }

    /**
     * get all members.
     *
     * @return all members
     */
    public Set<Endpoint> getAllMembers() {
        Set<Endpoint> endpoints = new HashSet<>(effectMembers.values());
        endpoints.addAll(lastMembers.values());
        return endpoints;
    }

    /**
     * get effect members.
     *
     * @return effect members
     */
    public Set<Endpoint> getEffectMembers() {
        return new HashSet<>(effectMembers.values());
    }

    /**
     * get last see members.
     *
     * @return all members
     */
    public Set<Endpoint> getLastMembers() {
        return new HashSet<>(lastMembers.values());
    }

    /**
     * As {@link MemberConfiguration#getAllMembers()} has no benchmark, remove param: removeId.
     *
     * @param removeId remove id.
     * @return get all members exclude param removeId
     */
    public Set<Endpoint> getMembersWithout(final String removeId) {
        return getAllMembers().stream().filter(it -> !StringUtils.equals(removeId, it.getId()))
                .collect(Collectors.toSet());
    }

    /**
     * check node is valid.
     *
     * @param nodeId check node id
     * @return is valid
     */
    public boolean isValid(final String nodeId) {
        return effectMembers.containsKey(nodeId) && lastMembers.containsKey(nodeId);
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
        if (effectMembers.containsKey(id)) {
            return effectMembers.get(id);
        }
        return lastMembers.getOrDefault(id, null);
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
        this.effectMembers.putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.version.incrementAndGet();
    }

    @Override
    public String toString() {
        return "MemberConfiguration{"
                + "version=" + version
                + ", allMembers=" + effectMembers
                + '}';
    }
}
