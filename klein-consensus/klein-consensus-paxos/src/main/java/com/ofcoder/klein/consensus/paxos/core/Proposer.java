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

import java.io.Serializable;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.Proposal;

/**
 * @author 释慧利
 */
public interface Proposer extends Lifecycle<ConsensusProp> {
    /**
     * Propose proposal.
     *
     * @param data client's data
     * @param done client's callback
     * @param <E>  client's data type, extend Serializable
     */
    <E extends Serializable> void propose(final String group, final E data, final ProposeDone done);

    /**
     * Boost instance
     *
     * @param instanceId id of instance for boost
     * @param done       callback
     */
    default void boost(final long instanceId, final ProposeDone done) {
        boost(instanceId, Proposal.NOOP, done);
    }

    void boost(final long instanceId, final Proposal proposal, final ProposeDone done);
}
