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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.sm.ElectionOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcEngine;

/**
 * @author 释慧利
 */
public class Master implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Master.class);
    private PaxosNode self;
    private RepeatedTimer electTimer;
    private RepeatedTimer heartbeatTimer;
    private RpcClient client;

    public Master(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        this.client = RpcEngine.getClient();

        // first run after 1 second, because the system may not be started
        electTimer = new RepeatedTimer("elect-master", 1000) {
            @Override
            protected void onTrigger() {
                election();
            }

            @Override
            protected int adjustTimeout(int timeoutMs) {
                return ThreadLocalRandom.current().nextInt(200, 500);
            }
        };

        heartbeatTimer = new RepeatedTimer("master-heartbeat", 100) {
            @Override
            protected void onTrigger() {
                heartbeat();
            }
        };

        electTimer.start();
    }

    @Override
    public void shutdown() {
        if (electTimer != null) {
            electTimer.destroy();
        }
        if (heartbeatTimer != null) {
            heartbeatTimer.destroy();
        }
    }

    private void election() {
        LOG.info("start electing master.");
        ElectionOp req = new ElectionOp();
        req.setNodeId(self.getSelf().getId());
        req.setMemberVersion(self.getMemberConfiguration().incrementVersion());

        CountDownLatch latch = new CountDownLatch(1);
        RoleAccessor.getProposer().propose(MasterSM.GROUP, req, new ProposeDone() {
            @Override
            public void negotiationDone(Result.State result) {
                if (result == Result.State.UNKNOWN) {
                    latch.countDown();
                }
            }

            @Override
            public void applyDone(Object result) {
                latch.countDown();
                electTimer.stop();
                heartbeatTimer.start();
            }
        });

        try {
            boolean await = latch.await(1L, TimeUnit.SECONDS);
            if (self.getMemberConfiguration().getMaster() != null) {
                electTimer.stop();
                if (self.getMemberConfiguration().getMaster() == self.getSelf()) {
                    heartbeatTimer.start();
                }
            }
        } catch (InterruptedException e) {
            LOG.warn("electing master timeout");
        }
    }

    private void heartbeat() {
        // 这里是发送心跳。
        // 另外，规定时间内，没有收到心跳，重新选举Master
    }

}
