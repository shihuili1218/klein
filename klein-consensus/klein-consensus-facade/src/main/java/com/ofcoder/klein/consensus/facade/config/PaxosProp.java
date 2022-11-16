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
package com.ofcoder.klein.consensus.facade.config;/**
 * @author far.liu
 */

import com.ofcoder.klein.common.util.SystemPropertyUtil;

/**
 * @author 释慧利
 */
public class PaxosProp {

    private int masterHeartbeatInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.heartbeat-interval", 50);
    private int masterElectMinInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.elect-min-interval", masterHeartbeatInterval * 10);
    private int masterElectMaxInterval = SystemPropertyUtil.getInt("klein.consensus.paxos.master.elect-max-interval", masterElectMinInterval + masterHeartbeatInterval * 3);

    private boolean onlyMasterWrite = SystemPropertyUtil.getBoolean("klein.consensus.paxos.master.writer", true);
    private boolean onlyMasterRead = SystemPropertyUtil.getBoolean("klein.consensus.paxos.master.reader", true);

    public int getMasterHeartbeatInterval() {
        return masterHeartbeatInterval;
    }

    public void setMasterHeartbeatInterval(int masterHeartbeatInterval) {
        this.masterHeartbeatInterval = masterHeartbeatInterval;
    }

    public int getMasterElectMinInterval() {
        return masterElectMinInterval;
    }

    public void setMasterElectMinInterval(int masterElectMinInterval) {
        this.masterElectMinInterval = masterElectMinInterval;
    }

    public int getMasterElectMaxInterval() {
        return masterElectMaxInterval;
    }

    public void setMasterElectMaxInterval(int masterElectMaxInterval) {
        this.masterElectMaxInterval = masterElectMaxInterval;
    }

    public boolean isOnlyMasterWrite() {
        return onlyMasterWrite;
    }

    public void setOnlyMasterWrite(boolean onlyMasterWrite) {
        this.onlyMasterWrite = onlyMasterWrite;
    }

    public boolean isOnlyMasterRead() {
        return onlyMasterRead;
    }

    public void setOnlyMasterRead(boolean onlyMasterRead) {
        this.onlyMasterRead = onlyMasterRead;
    }
}
