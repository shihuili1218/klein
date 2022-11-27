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

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Ping;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * @author 释慧利
 */
public interface Master extends Lifecycle<ConsensusProp> {

    /**
     * Change member, add <code>endpoint</code> to the cluster
     *
     * @param endpoint new member
     */
    void addMember(Endpoint endpoint);

    /**
     * Change member, remove <code>endpoint</code> from cluster
     *
     * @param endpoint delete member
     */
    void removeMember(Endpoint endpoint);

    /**
     * Election master
     */
    void electingMaster();

    /**
     * Processing heartbeat message.
     *
     * @param request heartbeat
     * @param isSelf  whether heartbeat come from themselves
     * @return whether accept the heartbeat
     */
    boolean onReceiveHeartbeat(final Ping request, boolean isSelf);

    /**
     * This is a callback method of master change.
     *
     * @param newMaster new master
     */
    void onChangeMaster(final String newMaster);

    /**
     * Added master health listener
     * @param listener listener
     */
    void addHealthyListener(final HealthyListener listener);

    interface HealthyListener {
        void change(boolean healthy);
    }

}
