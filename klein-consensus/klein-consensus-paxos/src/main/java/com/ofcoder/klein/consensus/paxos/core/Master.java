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

import com.ofcoder.klein.common.Role;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Ping;

/**
 * Master Role.
 *
 * @author 释慧利
 */
public interface Master extends Role<ConsensusProp> {

    /**
     * Get master information.
     *
     * @return master
     */
    MasterState getMaster();

    /**
     * Add a master change listener.
     *
     * @param listener lisener
     */
    void addListener(Listener listener);

    /**
     * Look for the master in the cluster for member startup.
     */
    void searchMaster();

    /**
     * Transfer the Master status to another member.
     */
    void transferMaster();

    /**
     * Processing heartbeat message.
     *
     * @param request heartbeat
     * @param isSelf  whether heartbeat come from themselves
     * @return whether accept the heartbeat
     */
    boolean onReceiveHeartbeat(Ping request, boolean isSelf);

    /**
     * handle NewMaster request.
     *
     * @param request msg data
     * @param isSelf  from self
     * @return handle result
     */
    NewMasterRes onReceiveNewMaster(NewMasterReq request, boolean isSelf);

    enum ElectState {
        DISABLE(true),
        ELECTING(false),
        FOLLOWING(true),
        LEADING(true);
        private final boolean allowPropose;

        ElectState(final boolean allowPropose) {
            this.allowPropose = allowPropose;
        }

        public boolean allowPropose() {
            return allowPropose;
        }

        public boolean allowBoost() {
            return this == DISABLE || this == LEADING;
        }
    }

    interface Listener {
        void onChange(MasterState master);
    }
}
