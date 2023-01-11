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

import com.ofcoder.klein.consensus.facade.Nwr;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * MemberManager.
 *
 * @author 释慧利
 */
public final class MemberRegistry {

    private final PaxosMemberConfiguration memberConfiguration = new PaxosMemberConfiguration();
    private Nwr nwr;

    private MemberRegistry() {
    }

    /**
     * init member configuration.
     *
     * @param members members
     * @param nwr     quorum calculator
     */
    public void init(final List<Endpoint> members, final Nwr nwr) {
        this.memberConfiguration.init(members);
        this.nwr = nwr;
    }

    public Nwr getNwr() {
        return nwr;
    }

    /**
     * load snapshot.
     *
     * @param snap snapshot
     */
    public void loadSnap(final PaxosMemberConfiguration snap) {
        memberConfiguration.loadSnap(snap);
    }

    /**
     * get member configuration.
     *
     * @return member configuration
     */
    public PaxosMemberConfiguration getMemberConfiguration() {
        return memberConfiguration;
    }

    public static MemberRegistry getInstance() {
        return MemberRegistryHolder.INSTANCE;
    }

    private static class MemberRegistryHolder {
        private static final MemberRegistry INSTANCE = new MemberRegistry();
    }

}
