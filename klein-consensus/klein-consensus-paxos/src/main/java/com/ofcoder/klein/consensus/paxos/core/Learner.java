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
import java.util.Set;

import com.ofcoder.klein.common.Role;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * Learner Role.
 *
 * @author 释慧利
 */
public interface Learner extends Role<ConsensusProp> {

    long getLastAppliedInstanceId();

    long getLastCheckpoint();

    Set<String> getGroups();

    /**
     * generate and save snapshot.
     *
     * @return snaps
     */
    Map<String, Snap> generateSnap();

    /**
     * load snap.
     *
     * @param snaps key: sm key
     *              value: snap
     */
    void loadSnapSync(Map<String, Snap> snaps);

    /**
     * replay log, re-enter instance into sm.
     *
     * @param group sm group name
     * @param start start instance id
     */
    void replayLog(String group, long start);

    /**
     * Load SM, one group will only load one SM.
     *
     * @param group group
     * @param sm    state machine
     */
    void loadSM(String group, SM sm);

    /**
     * Send confirm message.
     *
     * @param instanceId id of the instance
     * @param checksum   consensus data checksum
     * @param dons       call ProposeDone#applyDone(java.util.Map) when apply done
     */
    void confirm(long instanceId, String checksum, List<ProposalWithDone> dons);

    /**
     * Processing confirm message.
     * The confirm message is used to submit an instance.
     *
     * @param req    message
     * @param isSelf from self
     */
    void handleConfirmRequest(ConfirmReq req, boolean isSelf);

}
