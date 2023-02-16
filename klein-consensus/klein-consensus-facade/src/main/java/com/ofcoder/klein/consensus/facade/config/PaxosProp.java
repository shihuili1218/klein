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
package com.ofcoder.klein.consensus.facade.config;

import com.ofcoder.klein.common.util.SystemPropertyUtil;

/**
 * paxox property.
 *
 * @author 释慧利
 */
public class PaxosProp {
    private int masterHeartbeatTimeout = SystemPropertyUtil.getInt("klein.consensus.paxos.master.heartbeat-timeout", 50);
    private int masterHeartbeatInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.heartbeat-interval", 100);
    private int masterElectMinInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.elect-min-interval", masterHeartbeatInterval * 6);
    private int masterElectMaxInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.elect-max-interval", masterElectMinInterval + masterHeartbeatInterval * 3);
    private boolean write = SystemPropertyUtil.getBoolean("klein.consensus.paxos.write", true);

    public int getMasterHeartbeatTimeout() {
        return masterHeartbeatTimeout;
    }

    public void setMasterHeartbeatTimeout(final int masterHeartbeatTimeout) {
        this.masterHeartbeatTimeout = masterHeartbeatTimeout;
    }

    public int getMasterHeartbeatInterval() {
        return masterHeartbeatInterval;
    }

    public void setMasterHeartbeatInterval(final int masterHeartbeatInterval) {
        this.masterHeartbeatInterval = masterHeartbeatInterval;
    }

    public int getMasterElectMinInterval() {
        return masterElectMinInterval;
    }

    public void setMasterElectMinInterval(final int masterElectMinInterval) {
        this.masterElectMinInterval = masterElectMinInterval;
    }

    public int getMasterElectMaxInterval() {
        return masterElectMaxInterval;
    }

    public void setMasterElectMaxInterval(final int masterElectMaxInterval) {
        this.masterElectMaxInterval = masterElectMaxInterval;
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(final boolean write) {
        this.write = write;
    }

}
