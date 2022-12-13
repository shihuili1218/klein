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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.paxos.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public class MemberManager {
    private static final Logger LOG = LoggerFactory.getLogger(MemberManager.class);
    private static final PaxosMemberConfiguration configuration = new PaxosMemberConfiguration();
    private static volatile Endpoint self;

    public static int getVersion() {
        return configuration.getVersion().get();
    }

    public static Set<Endpoint> getAllMembers() {
        return new HashSet<>(configuration.getAllMembers().values());
    }

    public static Set<Endpoint> getMembersWithoutSelf() {
        final String selfId = self.getId();
        return getAllMembers().stream().filter(it -> !StringUtils.equals(selfId, it.getId()))
                .collect(Collectors.toSet());
    }

    public static Endpoint getEndpointById(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        return configuration.getAllMembers().getOrDefault(id, null);
    }

    public static boolean isValid(String nodeId) {
        return configuration.getAllMembers().containsKey(nodeId);
    }

    public static void init(List<Endpoint> nodes, Endpoint self) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        configuration.getAllMembers().putAll(nodes.stream().collect(Collectors.toMap(Endpoint::getId, Function.identity())));
        MemberManager.self = self;
        configuration.getVersion().incrementAndGet();
    }

    public static void writeOn(Endpoint node) {
        configuration.getAllMembers().put(node.getId(), node);
        configuration.getVersion().incrementAndGet();
    }

    public static void writeOff(Endpoint node) {
        configuration.getAllMembers().remove(node.getId());
        configuration.getVersion().incrementAndGet();
    }

    public static Endpoint getMaster() {
        return configuration.getMaster();
    }

    public static boolean changeMaster(String nodeId) {
        if (isValid(nodeId)) {
            configuration.setMaster(configuration.getAllMembers().get(nodeId));
            configuration.getVersion().incrementAndGet();

            RoleAccessor.getMaster().onChangeMaster(nodeId);
            LOG.info("node-{} was promoted to master, version: {}", nodeId, configuration.getVersion().get());
            return true;
        } else {
            return false;
        }
    }

    public static void loadSnap(PaxosMemberConfiguration snap) {
        configuration.setMaster(snap.getMaster());
        configuration.setVersion(snap.getVersion());
        configuration.setAllMembers(snap.getAllMembers());
    }

    public static PaxosMemberConfiguration createRef() {
        PaxosMemberConfiguration target = new PaxosMemberConfiguration();
        target.getAllMembers().putAll(configuration.getAllMembers());
        Endpoint master = configuration.getMaster();
        if (master != null) {
            target.setMaster(new Endpoint(master.getId(), master.getIp(), master.getPort()));
        }
        target.setVersion(configuration.getVersion());
        return target;
    }
}
