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

import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * Join Consensus Quorum.
 *
 * @author 释慧利
 */
public final class JoinConsensusQuorum implements Quorum {
    private final Quorum oldQuorum;
    private final Quorum newQuorum;

    private JoinConsensusQuorum(final Set<Endpoint> effectMembers, final Set<Endpoint> lasMemmbers, final int threshold) {
        oldQuorum = new SingleQuorum(effectMembers, threshold);
        newQuorum = new SingleQuorum(lasMemmbers, threshold);
    }

    /**
     * create new write Quorum checker.
     *
     * @param memberConfiguration member config
     * @return new Quorum checker
     */
    public static Quorum createWriteQuorum(final MemberConfiguration memberConfiguration) {
        Nwr nwr = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin();

        if (CollectionUtils.isEmpty(memberConfiguration.getLastMembers())) {
            return new SingleQuorum(memberConfiguration.getEffectMembers(),
                    nwr.w(memberConfiguration.getEffectMembers().size()));
        } else {
            return new JoinConsensusQuorum(memberConfiguration.getEffectMembers(), memberConfiguration.getLastMembers(),
                    nwr.w(memberConfiguration.getAllMembers().size()));
        }
    }

    /**
     * create new read Quorum checker.
     *
     * @param memberConfiguration member config
     * @return new Quorum checker
     */
    public static Quorum createReadQuorum(final MemberConfiguration memberConfiguration) {
        Nwr nwr = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin();

        if (CollectionUtils.isEmpty(memberConfiguration.getLastMembers())) {
            return new SingleQuorum(memberConfiguration.getEffectMembers(),
                    nwr.r(memberConfiguration.getEffectMembers().size()));
        } else {
            return new JoinConsensusQuorum(memberConfiguration.getEffectMembers(), memberConfiguration.getLastMembers(),
                    nwr.r(memberConfiguration.getAllMembers().size()));
        }
    }

    @Override
    public boolean refuse(final Endpoint node) {
        oldQuorum.refuse(node);
        newQuorum.refuse(node);
        return true;
    }

    @Override
    public boolean grant(final Endpoint node) {
        oldQuorum.grant(node);
        newQuorum.grant(node);
        return true;
    }

    @Override
    public GrantResult isGranted() {
        if (oldQuorum.isGranted() == GrantResult.REFUSE || newQuorum.isGranted() == GrantResult.REFUSE) {
            return GrantResult.REFUSE;
        }
        if (oldQuorum.isGranted() == GrantResult.PASS && newQuorum.isGranted() == GrantResult.PASS) {
            return GrantResult.PASS;
        }
        return GrantResult.GRANTING;
    }
}
