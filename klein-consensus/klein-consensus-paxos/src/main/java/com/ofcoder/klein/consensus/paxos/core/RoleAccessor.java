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

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;

/**
 * Role Accessor.
 *
 * @author 释慧利
 */
public class RoleAccessor {

    private static Proposer proposer;
    private static Acceptor acceptor;
    private static Learner learner;
    private static Master master;

    public static Proposer getProposer() {
        return proposer;
    }

    public static Acceptor getAcceptor() {
        return acceptor;
    }

    public static Learner getLearner() {
        return learner;
    }

    public static Master getMaster() {
        return master;
    }

    /**
     * create master, learner, acceptor, proposer.
     *
     * @param prop property
     * @param self node information
     */
    public static void create(final ConsensusProp prop, final PaxosNode self) {
        initMaster(prop, self);
        initLearner(prop, self);
        initAcceptor(prop, self);
        initProposer(prop, self);
    }

    private static void initMaster(final ConsensusProp prop, final PaxosNode self) {
        master = new MasterImpl(self);
        master.init(prop);
    }

    private static void initLearner(final ConsensusProp prop, final PaxosNode self) {
        learner = new LearnerImpl(self);
        learner.init(prop);
    }

    private static void initAcceptor(final ConsensusProp prop, final PaxosNode self) {
        acceptor = new AcceptorImpl(self);
        acceptor.init(prop);
    }

    private static void initProposer(final ConsensusProp prop, final PaxosNode self) {
        proposer = new ProposerImpl(self);
        proposer.init(prop);
    }

}
