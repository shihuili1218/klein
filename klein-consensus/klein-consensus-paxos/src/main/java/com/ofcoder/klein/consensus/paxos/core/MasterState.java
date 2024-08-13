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

import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Master Info.
 */
public class MasterState {
    private final Endpoint master;
    private final Master.ElectState electState;
    /**
     * Whether I am a Master. true if I am master.
     */
    private final boolean isSelf;

    public MasterState(final Endpoint master, final Master.ElectState electState, final boolean isSelf) {
        this.master = master;
        this.electState = electState;
        this.isSelf = isSelf;
    }

    public Endpoint getMaster() {
        return master;
    }

    public Master.ElectState getElectState() {
        return electState;
    }

    public boolean isSelf() {
        return isSelf;
    }

    @Override
    public String toString() {
        return "MasterState{"
                + "master=" + master
                + ", electState=" + electState
                + ", isSelf=" + isSelf
                + '}';
    }
}
