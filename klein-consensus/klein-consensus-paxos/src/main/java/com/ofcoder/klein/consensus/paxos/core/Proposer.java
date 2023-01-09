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
package com.ofcoder.klein.consensus.paxos.core;

import java.util.List;

import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;

/**
 * Proposer Role.
 *
 * @author 释慧利
 */
public interface Proposer extends Lifecycle<ConsensusProp> {
    /**
     * Propose proposal.
     *
     * @param data client's data
     * @param done client's callback
     */
    void propose(Proposal data, ProposeDone done);

    /**
     * Try to boost instance.
     * Boost the copy of the proposal to the majority and the confirm status is reached
     *
     * @param instanceHolder  id of the instance that you want to boost
     * @param done            boost callback, NOTICE: it may be called multiple times
     * @param defaultProposal default proposal
     */
    void tryBoost(Holder<Long> instanceHolder, List<Proposal> defaultProposal, ProposeDone done);

    /**
     * Try to boost instance.
     * Boost the copy of the proposal to the majority and the confirm status is reached
     *
     * @param instanceHolder      id of the instance that you want to boost
     * @param done                boost callback, NOTICE: it may be called multiple times
     * @param defaultProposal     default proposal
     * @param memberConfiguration member config
     */
    void tryBoost(Holder<Long> instanceHolder, PaxosMemberConfiguration memberConfiguration, List<Proposal> defaultProposal, ProposeDone done);

}
