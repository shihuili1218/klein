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
package com.ofcoder.klein.consensus.paxos;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.Cluster;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.core.ProposeDone;
import com.ofcoder.klein.consensus.paxos.core.RoleAccessor;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * Paxos Cluster info.
 *
 * @author 释慧利
 */
@Join
public class PaxosCluster implements Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(PaxosCluster.class);
    private ConsensusProp prop;
    private PaxosMemberConfiguration memberConfiguration;
    private final AtomicBoolean changing = new AtomicBoolean(false);

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        loadMemberConfig();
    }

    private void loadMemberConfig() {
        // reload member config from storage.
        LogManager<Proposal> logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        PaxosMemberConfiguration defaultValue = new PaxosMemberConfiguration();
        defaultValue.init(prop.getMembers());

        this.memberConfiguration = (PaxosMemberConfiguration) logManager.loadCluster(defaultValue);

        LOG.info("member config: {}", memberConfiguration);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public MemberConfiguration getMemberConfig() {
        return this.memberConfiguration;
    }

    @Override
    public void addMember(final Endpoint endpoint) {
        if (memberConfiguration.isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.ADD, endpoint);
    }

    @Override
    public void removeMember(final Endpoint endpoint) {
        if (!memberConfiguration.isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.REMOVE, endpoint);
    }

    private void changeMember(final byte op, final Endpoint endpoint) {
        LOG.info("start add member.");
        // stop → change member → restart → propose noop.

        try {
            // It can only be changed once at a time
            if (!changing.compareAndSet(false, true)) {
                return;
            }

            ChangeMemberOp req = new ChangeMemberOp();
            req.setNodeId(prop.getSelf().getId());
            req.setTarget(endpoint);
            req.setOp(op);

            CountDownLatch latch = new CountDownLatch(1);
            RoleAccessor.getProposer().propose(new Proposal(MasterSM.GROUP, req), new ProposeDone() {
                @Override
                public void negotiationDone(final boolean result, final List<Proposal> consensusDatas) {
                    if (!result) {
                        latch.countDown();
                    }
                }

                @Override
                public void applyDone(final Map<Proposal, Object> applyResults) {
                    latch.countDown();
                }
            });
            boolean await = latch.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
            // do nothing for await.result
        } catch (InterruptedException e) {
            // do nothing
        } finally {
            changing.compareAndSet(true, false);
        }
    }

}
