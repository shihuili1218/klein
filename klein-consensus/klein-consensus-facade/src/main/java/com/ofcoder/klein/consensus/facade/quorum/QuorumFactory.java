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
package com.ofcoder.klein.consensus.facade.quorum;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.nwr.Nwr;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * Create Quorum Factory.
 *
 * @author 释慧利
 */
public final class QuorumFactory {
    private static final Logger LOG = LoggerFactory.getLogger(QuorumFactory.class);

    /**
     * create new write Quorum checker.
     *
     * @param memberConfiguration member config
     * @return new Quorum checker
     */
    public static Quorum createWriteQuorum(final MemberConfiguration memberConfiguration) {
        Nwr nwr = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin();
        if (LOG.isDebugEnabled()) {
            LOG.debug("create write quorum, nwr: {}", nwr.getClass());
        }
        if (CollectionUtils.isEmpty(memberConfiguration.getLastMembers())) {
            Set<Endpoint> participants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());
            return new SingleQuorum(participants, nwr.w(participants.size()));
        } else {
            Set<Endpoint> effectParticipants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());
            Set<Endpoint> lastParticipants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());

            return new JoinConsensusQuorum(effectParticipants, lastParticipants,
                    nwr.w(effectParticipants.size()), nwr.w(lastParticipants.size()));
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
        LOG.debug("create read quorum, nwr: {}", nwr.getClass());

        if (CollectionUtils.isEmpty(memberConfiguration.getLastMembers())) {
            Set<Endpoint> participants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());
            return new SingleQuorum(participants, nwr.r(participants.size()));
        } else {
            Set<Endpoint> effectParticipants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());
            Set<Endpoint> lastParticipants = memberConfiguration.getEffectMembers().stream().filter(endpoint -> !endpoint.isOutsider()).collect(Collectors.toSet());

            return new JoinConsensusQuorum(effectParticipants, lastParticipants,
                    nwr.r(effectParticipants.size()), nwr.r(lastParticipants.size()));
        }
    }
}
