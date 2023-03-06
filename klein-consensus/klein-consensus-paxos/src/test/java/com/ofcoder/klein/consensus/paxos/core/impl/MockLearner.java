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
package com.ofcoder.klein.consensus.paxos.core.impl;

import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.ProposalWithDone;
import com.ofcoder.klein.consensus.paxos.handler.SnapshotHandler;
import com.ofcoder.klein.consensus.paxos.rpc.vo.*;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.storage.facade.Snap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * mock Learner Role.
 *
 * @author 李航
 */
public class MockLearner extends SnapshotHandler {
    public PaxosNode getSelf() {
        return self;
    }
    /**
     * snap count
     */
    public static volatile int GENERATE_SNAP_COUNT = 0;
    /**
     * write count
     */
    public static volatile int WRITE_COUNT = 0;

    public MockLearner(PaxosNode paxosNode) {
        super(paxosNode, TimeUnit.SECONDS);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public Map<String, Snap> generateSnap() {
        GENERATE_SNAP_COUNT++;
        self.updateLastCheckpoint(WRITE_COUNT);
        return null;
    }

    @Override
    public void loadSnap(Map<String, Snap> snaps) {

    }

    @Override
    public void replayLog(String group, long start) {

    }

    @Override
    public long getLastAppliedInstanceId() {
        return WRITE_COUNT;
    }

    @Override
    public void loadSM(String group, SM sm) {

    }

    @Override
    public void learn(long instanceId, Endpoint target, LearnCallback callback) {

    }

    @Override
    public void confirm(long instanceId, String checksum, List<ProposalWithDone> dons) {

    }

    @Override
    public void pullSameData(NodeState state) {

    }

    @Override
    public void pushSameData(Endpoint target) {

    }

    @Override
    public boolean healthy() {
        return false;
    }

    @Override
    public void handleConfirmRequest(ConfirmReq req, boolean isSelf) {

    }

    @Override
    public LearnRes handleLearnRequest(LearnReq req) {
        WRITE_COUNT++;
        return null;
    }

    @Override
    public SnapSyncRes handleSnapSyncRequest(SnapSyncReq req) {
        return null;
    }
}
