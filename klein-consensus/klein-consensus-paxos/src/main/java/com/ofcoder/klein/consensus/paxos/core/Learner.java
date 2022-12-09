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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncRes;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public interface Learner extends Lifecycle<ConsensusProp> {

    /**
     * Load SM, one group will only load one SM
     *
     * @param group group
     * @param sm    state machine
     */
    void loadSM(final String group, final SM sm);

    /**
     * Send the learn message to <code>target</code>
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @param callback   Callbacks of learning results
     */
    void learn(long instanceId, Endpoint target, LearnCallback callback);

    /**
     * Send the learn message to <code>target</code>
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @see Learner#learn(long, Endpoint, LearnCallback)
     */
    default void learn(long instanceId, Endpoint target) {
        learn(instanceId, target, new DefaultLearnCallback());
    }

    /**
     * Synchronous learning
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
     * @param callback   apply callback
     */
    void confirm(long instanceId, final ApplyCallback callback);

    /**
     * Keep the data consistent with <code>target</code>
     *
     * @param target               to be learned
     * @param checkpoint           checkpoint of the last snapshot of <code>target</code>
     * @param maxAppliedInstanceId maxAppliedInstanceId of <code>target</code>
     */
    void keepSameData(final Endpoint target, final long checkpoint, final long maxAppliedInstanceId);

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
     */
    LearnRes handleLearnRequest(LearnReq req);

    /**
     * Processing Snapshot Synchronization message.
     *
     * @param req message
     */
    SnapSyncRes handleSnapSyncRequest(SnapSyncReq req);

    interface ApplyCallback {
        void apply(Proposal input, Object output);
    }


    interface LearnCallback {
        void learned(boolean result);
    }

    class DefaultLearnCallback implements LearnCallback {
        @Override
        public void learned(boolean result) {

        }
    }

    class DefaultApplyCallback implements ApplyCallback {
        @Override
        public void apply(Proposal input, Object output) {
            // do nothing.
        }
    }


}
