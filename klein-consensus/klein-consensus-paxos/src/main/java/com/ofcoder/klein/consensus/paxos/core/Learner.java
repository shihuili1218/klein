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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * Learner Role.
 *
 * @author 释慧利
 */
public interface Learner extends Lifecycle<ConsensusProp> {

    /**
     * generate and save snapshot.
     *
     * @return snaps
     */
    Map<String, Snap> generateSnap();

    /**
     * load snap.
     *
     * @param snaps snaps
     */
    void loadSnap(Map<String, Snap> snaps);

    /**
     * Load SM, one group will only load one SM.
     *
     * @param group group
     * @param sm    state machine
     */
    void loadSM(String group, SM sm);

    /**
     * Send the learn message to <code>target</code>.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @param callback   Callbacks of learning results
     */
    void learn(long instanceId, Endpoint target, LearnCallback callback);

    /**
     * Send the learn message to <code>target</code>.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @see Learner#learn(long, Endpoint, LearnCallback)
     */
    default void learn(long instanceId, Endpoint target) {
        learn(instanceId, target, new DefaultLearnCallback());
    }

    /**
     * Synchronous learning.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @return learn result
     * @see Learner#learn(long, Endpoint, LearnCallback)
     */
    default boolean learnSync(long instanceId, Endpoint target) {
        long singleTimeoutMS = 150;
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        learn(instanceId, target, future::complete);
        try {
            return future.get(singleTimeoutMS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send confirm message.
     *
     * @param instanceId id of the instance
     * @param dons       call ProposeDone#applyDone(java.util.Map) when apply done
     */
    void confirm(long instanceId, List<ProposeDone> dons);

    /**
     * Keep the data consistent with master, state is master.
     * Caller is slave.
     *
     * @param state target information
     */
    void pullSameData(NodeState state);

    /**
     * Master pushes data to slave, target is slave.
     * Caller is master.
     *
     * @param target target
     */
    void pushSameData(Endpoint target);

    /**
     * Keep consistent with the data in the cluster.
     *
     * @return <code>true:</code> same data, <code>false:</code>: not the same
     */
    boolean healthy();

    /**
     * Processing confirm message.
     * The confirm message is used to submit an instance.
     *
     * @param req message
     */
    void handleConfirmRequest(ConfirmReq req);

    /**
     * Processing learn message.
     * Other members learn the specified instance from themselves.
     *
     * @param req message
     * @return handle result
     */
    LearnRes handleLearnRequest(LearnReq req);

    /**
     * Processing Snapshot Synchronization message.
     *
     * @param req message
     * @return handle result
     */
    SnapSyncRes handleSnapSyncRequest(SnapSyncReq req);

    interface LearnCallback {
        void learned(boolean result);
    }

    class DefaultLearnCallback implements LearnCallback {
        @Override
        public void learned(final boolean result) {

        }
    }

}
