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
 * @author 释慧利
 */
public class MemberManager {
    private static final Logger LOG = LoggerFactory.getLogger(MemberManager.class);
    private static final PaxosMemberConfiguration configuration = new PaxosMemberConfiguration();
    private static volatile Endpoint self;

    public static void init(List<Endpoint> nodes, Endpoint self) {
        configuration.init(nodes);
        MemberManager.self = self;
    }

    public static void loadSnap(PaxosMemberConfiguration snap) {
        configuration.loadSnap(snap);
    }

    public static boolean changeMaster(String nodeId) {
        return configuration.changeMaster(nodeId);
    }

    public static void writeOn(Endpoint node) {
        configuration.writeOn(node);
    }

    public static void writeOff(Endpoint node) {
        configuration.writeOff(node);
    }

    public static Endpoint getEndpointById(String id) {
        return configuration.getEndpointById(id);
    }

    public static PaxosMemberConfiguration createRef() {
        return configuration.createRef();
    }

    public static Set<Endpoint> getAllMembers() {
        return configuration.getAllMembers();
    }

    public static boolean isValid(String nodeId) {
        return configuration.isValid(nodeId);
    }
}
