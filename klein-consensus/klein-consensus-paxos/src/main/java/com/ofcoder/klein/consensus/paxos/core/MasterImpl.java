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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Quorum;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.PaxosQuorum;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.ElectionOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Ping;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Pong;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;

/**
 * @author 释慧利
 */
public class MasterImpl implements Master {
    private static final Logger LOG = LoggerFactory.getLogger(MasterImpl.class);
    private final PaxosNode self;
    private RepeatedTimer electTimer;
    private RepeatedTimer sendHeartbeatTimer;
    private RpcClient client;
    private ConsensusProp prop;
    private final AtomicBoolean electing = new AtomicBoolean(false);
    private final AtomicBoolean changing = new AtomicBoolean(false);
    private final List<HealthyListener> listeners = new ArrayList<>();
    private LogManager<Proposal> logManager;
    private ElectState state = ElectState.ELECTING;

    public MasterImpl(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void shutdown() {
        if (electTimer != null) {
            electTimer.destroy();
        }
        if (sendHeartbeatTimer != null) {
            sendHeartbeatTimer.destroy();
        }
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        electTimer = new RepeatedTimer("elect-master", calculateElectionMasterInterval()) {
            @Override
            protected void onTrigger() {
                election();
            }

            @Override
            protected int adjustTimeout(int timeoutMs) {
                return calculateElectionMasterInterval();
            }
        };

        sendHeartbeatTimer = new RepeatedTimer("master-heartbeat", prop.getPaxosProp().getMasterHeartbeatInterval()) {
            @Override
            protected void onTrigger() {
                sendHeartbeat();
            }
        };
    }

    private int calculateElectionMasterInterval() {
        return ThreadLocalRandom.current().nextInt(prop.getPaxosProp().getMasterElectMinInterval(), prop.getPaxosProp().getMasterElectMaxInterval());
    }

    @Override
    public void addMember(Endpoint endpoint) {
        if (self.getMemberConfiguration().isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.ADD, endpoint);
    }

    @Override
    public void removeMember(Endpoint endpoint) {
        if (!self.getMemberConfiguration().isValid(endpoint.getId())) {
            return;
        }
        changeMember(ChangeMemberOp.REMOVE, endpoint);
    }

    private void changeMember(byte op, Endpoint endpoint) {
        LOG.info("start add member.");

        try {
            // It can only be changed once at a time
            if (!changing.compareAndSet(false, true)) {
                return;
            }

            ChangeMemberOp req = new ChangeMemberOp();
            req.setNodeId(self.getSelf().getId());
            req.setTarget(endpoint);
            req.setOp(op);

            CountDownLatch latch = new CountDownLatch(1);
            RoleAccessor.getProposer().tryBoost(self.incrementInstanceId(), Lists.newArrayList(new Proposal(MasterSM.GROUP, req))
                    , new ProposeDone() {
                        @Override
                        public void negotiationDone(boolean result, List<Proposal> consensusDatas) {
                            if (!result) {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void applyDone(Map<Proposal, Object> applyResults) {
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

    @Override
    public void electingMaster() {
        restartElect();
    }

    private void election() {
        LOG.debug("timer state, elect: {}, heartbeat: {}", electTimer.isRunning(), sendHeartbeatTimer.isRunning());

        updateMasterState(ElectState.ELECTING);
        if (!electing.compareAndSet(false, true)) {
            return;
        }

        try {
            LOG.info("start electing master.");
            ElectionOp req = new ElectionOp();
            req.setNodeId(self.getSelf().getId());

            CountDownLatch latch = new CountDownLatch(1);
            Proposal proposal = new Proposal(MasterSM.GROUP, req);
            RoleAccessor.getProposer().tryBoost(self.incrementInstanceId(), Lists.newArrayList(proposal)
                    , new ProposeDone() {
                        @Override
                        public void negotiationDone(boolean result, List<Proposal> consensusDatas) {
                            LOG.info("electing master, negotiationDone: {}", result);
                            if (result && consensusDatas.contains(proposal)) {
                                ThreadExecutor.submit(MasterImpl.this::boostInstance);
                            } else {
                                latch.countDown();
                            }
                        }

                        @Override
                        public void applyDone(Map<Proposal, Object> applyResults) {
                            LOG.info("electing master, applyDone: {}", applyResults);
                            latch.countDown();
                        }
                    });

            try {
                boolean await = latch.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
                // do nothing for await's result, stop this timer in {@link #onChangeMaster}
            } catch (InterruptedException e) {
                LOG.debug(e.getMessage());
            }
        } finally {
            electing.compareAndSet(true, false);
        }
    }

    private void boostInstance() {
        updateMasterState(ElectState.BOOSTING);

        stopAllTimer();

        PaxosMemberConfiguration memberConfiguration = self.getMemberConfiguration();
        NewMasterReq req = NewMasterReq.Builder.aNewMasterReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();
        PaxosQuorum quorum = PaxosQuorum.createInstance(memberConfiguration);
        AtomicBoolean next = new AtomicBoolean(false);

        // for self
        quorum.grant(self.getSelf());

        // for other members
        memberConfiguration.getMembersWithoutSelf().forEach(it ->
                client.sendRequestAsync(it, req, new AbstractInvokeCallback<NewMasterRes>() {
                    @Override
                    public void error(Throwable err) {
                        quorum.refuse(it);
                        if (quorum.isGranted() == Quorum.GrantResult.REFUSE && next.compareAndSet(false, true)) {
                            restartElect();
                        }
                    }

                    @Override
                    public void complete(NewMasterRes result) {
                        self.updateCurInstanceId(result.getCurInstanceId());
                        quorum.grant(it);
                        if (quorum.isGranted() == Quorum.GrantResult.PASS && next.compareAndSet(false, true)) {
                            ThreadExecutor.submit(MasterImpl.this::_boosting);
                        }
                    }
                }, 50L));
    }

    private void _boosting() {
        long miniInstanceId = self.getCurAppliedInstanceId();
        long maxInstanceId = self.getCurInstanceId();
        for (; miniInstanceId < maxInstanceId; miniInstanceId++) {
            Instance<Proposal> instance = logManager.getInstance(miniInstanceId);
            if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
                continue;
            }
            List<Proposal> grantedValue = instance != null ? instance.getGrantedValue() : Lists.newArrayList(Proposal.NOOP);
            grantedValue = CollectionUtils.isNotEmpty(grantedValue) ? grantedValue : Lists.newArrayList(Proposal.NOOP);
            RoleAccessor.getProposer().tryBoost(miniInstanceId, grantedValue, new ProposeDone.DefaultProposeDone());
        }
    }

    private void sendHeartbeat() {
        updateMasterState(ElectState.DOMINANT);

        final PaxosMemberConfiguration memberConfiguration = self.getMemberConfiguration().createRef();
        final Quorum quorum = PaxosQuorum.createInstance(memberConfiguration);
        final Ping req = Ping.Builder.aPing()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .maxAppliedInstanceId(self.getCurAppliedInstanceId())
                .lastCheckpoint(self.getLastCheckpoint())
                .maxInstanceId(self.getCurInstanceId())
                .build();

        final CompletableFuture<Quorum.GrantResult> complete = new CompletableFuture<>();
        // for self
        if (onReceiveHeartbeat(req, true)) {
            quorum.grant(self.getSelf());
        }

        // for other members
        memberConfiguration.getMembersWithoutSelf().forEach(it -> {
            client.sendRequestAsync(it, req, new AbstractInvokeCallback<Pong>() {
                @Override
                public void error(Throwable err) {
                    LOG.debug("node: " + it.getId() + ", " + err.getMessage());
                    quorum.refuse(it);
                    if (quorum.isGranted() == Quorum.GrantResult.REFUSE) {
                        complete.complete(quorum.isGranted());
                    }
                }

                @Override
                public void complete(Pong result) {
                    quorum.grant(it);
                    if (quorum.isGranted() == Quorum.GrantResult.PASS) {
                        complete.complete(quorum.isGranted());
                    }
                }
            }, 55);
        });
        try {
            Quorum.GrantResult grantResult = complete.get(60L, TimeUnit.MILLISECONDS);
            if (grantResult != Quorum.GrantResult.PASS) {
                restartElect();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            restartElect();
        }
    }

    @Override
    public void addHealthyListener(HealthyListener listener) {
        listeners.add(listener);
    }

    @Override
    public ElectState electState() {
        return state;
    }

    private void updateMasterState(ElectState healthy) {
        state = healthy;
        listeners.forEach(it -> it.change(healthy));
    }

    @Override
    public boolean onReceiveHeartbeat(Ping request, boolean isSelf) {
        final PaxosMemberConfiguration memberConfiguration = self.getMemberConfiguration();

        self.updateCurInstanceId(request.getMaxInstanceId());

        // check and update instance
        checkAndUpdateInstance(request);

        if ((memberConfiguration.getMaster() == null
                || (memberConfiguration.getMaster() != null && StringUtils.equals(request.getNodeId(), memberConfiguration.getMaster().getId())))
                && request.getMemberConfigurationVersion() >= memberConfiguration.getVersion()) {

            // reset and restart election timer
            if (!isSelf) {
                updateMasterState(ElectState.FOLLOWING);
                restartElect();
            }
            LOG.info("receive heartbeat from node-{}, result: true.", request.getNodeId());
            return true;
        } else {
            LOG.info("receive heartbeat from node-{}, result: false. local.master: {}, req.version: {}", request.getNodeId()
                    , memberConfiguration, request.getMemberConfigurationVersion());
            return false;
        }
    }

    @Override
    public NewMasterRes onReceiveNewMaster(NewMasterReq request, boolean isSelf) {
        return NewMasterRes.Builder.aNewMasterRes()
                .checkpoint(self.getLastCheckpoint())
                .curInstanceId(self.getCurInstanceId())
                .lastAppliedId(self.getCurAppliedInstanceId())
                .build();
    }

    private void checkAndUpdateInstance(Ping request) {
        Endpoint from = self.getMemberConfiguration().getEndpointById(request.getNodeId());
        ThreadExecutor.submit(() -> {
            RoleAccessor.getLearner().keepSameData(from, request.getLastCheckpoint(), request.getMaxAppliedInstanceId());
        });
    }

    private void stopAllTimer() {
        sendHeartbeatTimer.stop();
        electTimer.stop();
    }

    private void restartElect() {
        sendHeartbeatTimer.stop();
        electTimer.restart();
    }

    private void restartHeartbeat() {
        electTimer.stop();
        sendHeartbeatTimer.restart();
    }

    @Override
    public void onChangeMaster(final String newMaster) {
        if (StringUtils.equals(newMaster, self.getSelf().getId())) {
            restartHeartbeat();
        } else {
            restartElect();
        }
    }
}
