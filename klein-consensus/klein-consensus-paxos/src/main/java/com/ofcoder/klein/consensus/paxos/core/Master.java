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

import java.util.concurrent.ThreadLocalRandom;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.util.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;

/**
 * @author 释慧利
 */
public class Master implements Lifecycle<ConsensusProp> {
    private PaxosNode self;

    public Master(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        // first run after 1 second, because the system may not be started
        RepeatedTimer timer = new RepeatedTimer("elect-master", 1000) {
            @Override
            protected void onTrigger() {
                election();
            }

            @Override
            protected int adjustTimeout(int timeoutMs) {
                return ThreadLocalRandom.current().nextInt(200, 500);
            }
        };
    }

    @Override
    public void shutdown() {

    }


    private void election() {

    }

    private int randomTimeout(final int timeoutMs) {
        return ThreadLocalRandom.current().nextInt(timeoutMs, timeoutMs + 300);
    }
}
