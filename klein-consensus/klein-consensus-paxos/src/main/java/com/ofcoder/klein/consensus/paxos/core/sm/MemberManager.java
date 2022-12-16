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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * member manager.
 *
 * @author 释慧利
 */
public class MemberManager {
    private static final Logger LOG = LoggerFactory.getLogger(MemberManager.class);
    private static final PaxosMemberConfiguration CONFIGURATION = new PaxosMemberConfiguration();
    private static volatile Endpoint self;

    /**
     * init configuration.
     *
     * @param nodes all members
     * @param self  self information
     */
    public static void init(final List<Endpoint> nodes, final Endpoint self) {
        CONFIGURATION.init(nodes);
        MemberManager.self = self;
    }

    /**
     * load snapshot.
     *
     * @param snap snapshot
     */
    public static void loadSnap(final PaxosMemberConfiguration snap) {
        CONFIGURATION.loadSnap(snap);
    }

    /**
     * change master.
     *
     * @param nodeId new master id
     * @return change result
     */
    public static boolean changeMaster(final String nodeId) {
        return CONFIGURATION.changeMaster(nodeId);
    }

    /**
     * add member.
     *
     * @param node new member
     */
    public static void writeOn(final Endpoint node) {
        CONFIGURATION.writeOn(node);
    }

    /**
     * remove member.
     *
     * @param node error member
     */
    public static void writeOff(final Endpoint node) {
        CONFIGURATION.writeOff(node);
    }

    /**
     * get endpoint info by node id.
     *
     * @param id node id
     * @return endpoint
     */
    public static Endpoint getEndpointById(final String id) {
        return CONFIGURATION.getEndpointById(id);
    }

    /**
     * create an object with the same data.
     *
     * @return new reference
     */
    public static PaxosMemberConfiguration createRef() {
        return CONFIGURATION.createRef();
    }

    /**
     * get all members.
     *
     * @return all members
     */
    public static Set<Endpoint> getAllMembers() {
        return CONFIGURATION.getAllMembers();
    }

    /**
     * check node is valid.
     *
     * @param nodeId check node id
     * @return is valid
     */
    public static boolean isValid(final String nodeId) {
        return CONFIGURATION.isValid(nodeId);
    }
}
