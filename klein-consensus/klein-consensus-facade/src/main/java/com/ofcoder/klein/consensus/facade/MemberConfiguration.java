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

import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * MemberConfiguration.
 * <p>
 * 如果所有成员都支持成员变更，那必定不支持并行成员变更
 * 初始状态{A, B, C}, version = 1
 * A 变更为 {A, B, C, D} version = 2，AB达成共识
 * B 变更为 {A, B, C, E} version = 2，重试后，ABC达成共识，那么D消失了
 *
 * @author far.liu
 */
public class MemberConfiguration implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(MemberConfiguration.class);
    private static final Object CHANGE_LOCK = new Object();
    protected AtomicInteger version = new AtomicInteger(0);
    protected Map<String, Endpoint> effectMembers = new ConcurrentHashMap<>();
    protected Map<String, Endpoint> lastMembers = new ConcurrentHashMap<>();
    protected Endpoint self;

    public int getVersion() {
        return version.get();
    }

    /**
     * Start joint consensus changes, a replica should be used to call this method, which will not take effect on the current configuration.
     *
     * @return new config
     */
    public Set<Endpoint> startJoinConsensus(final List<Endpoint> add, final List<Endpoint> remove) {
        Set<Endpoint> newConfig = new HashSet<>(getLastMembers());
        newConfig.addAll(getEffectMembers());

        if (add != null) {
            newConfig.addAll(add.stream().filter(it -> !isValid(it.getId())).collect(Collectors.toList()));
        }
        if (remove != null) {
            newConfig.removeAll(remove.stream().filter(it -> isValid(it.getId())).collect(Collectors.toList()));
        }

        return newConfig;
    }

    /**
     * Set the last seen configuration.
     * Call the method by {@link SM} after the first phase of the joint consensus, and increment {@link MemberConfiguration#version}.
     *
     * @param newConfig new configuration
     */
    public void seenNewConfig(final Set<Endpoint> newConfig) {
        LOG.debug("see a new configuration, local.version: {}, old: {}, new: {}, lastMembers: {}", version, effectMembers, newConfig, lastMembers);

        this.lastMembers.clear();
        this.lastMembers.putAll(newConfig.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.version.incrementAndGet();
    }

    /**
     * Commit the last seen configuration.
     * Call the method by {@link SM} after the second phase of the joint consensus.
     *
     * @param newConfig new configuration
     */
    public void commitNewConfig(final Set<Endpoint> newConfig) {
        LOG.debug("commit a new configuration, local.version: {}, new: {}, lastMembers: {} ", version, newConfig, lastMembers);

        this.effectMembers = new ConcurrentHashMap<>(newConfig.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        this.lastMembers.clear();
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
        return effectMembers.containsKey(nodeId) || lastMembers.containsKey(nodeId);
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
     * @param self  self
     * @param nodes all members
     */
    public void init(final Endpoint self, final List<Endpoint> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        this.self = self;
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
