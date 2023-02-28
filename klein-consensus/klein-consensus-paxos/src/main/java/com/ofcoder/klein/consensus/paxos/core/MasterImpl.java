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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.common.exception.ShutdownException;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.ChangeMemberException;
import com.ofcoder.klein.consensus.facade.exception.ConsensusException;
import com.ofcoder.klein.consensus.facade.nwr.Nwr;
import com.ofcoder.klein.consensus.facade.quorum.Quorum;
import com.ofcoder.klein.consensus.facade.quorum.QuorumFactory;
import com.ofcoder.klein.consensus.facade.quorum.SingleQuorum;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.ChangeMemberOp;
import com.ofcoder.klein.consensus.paxos.core.sm.ElectionOp;
import com.ofcoder.klein.consensus.paxos.core.sm.MasterSM;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NewMasterRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Ping;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Pong;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PreElectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PreElectRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PushCompleteDataReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PushCompleteDataRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.RedirectRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * Master implement.
 *
 * @author 释慧利
 */
public class MasterImpl implements Master {
    private static final Logger LOG = LoggerFactory.getLogger(MasterImpl.class);
    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private RepeatedTimer waitHeartbeatTimer;
    private RepeatedTimer sendHeartbeatTimer;
    private RepeatedTimer electTimer;
    private RpcClient client;
    private ConsensusProp prop;
    private final AtomicBoolean electing = new AtomicBoolean(false);
    private LogManager<Proposal> logManager;
    private final AtomicBoolean changing = new AtomicBoolean(false);
    private final Nwr nwr;

    public MasterImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
        this.nwr = ExtensionLoader.getExtensionLoader(Nwr.class).getJoin();
    }

    @Override
    public void shutdown() {
        try {
            if (waitHeartbeatTimer != null) {
                waitHeartbeatTimer.destroy();
            }
            if (sendHeartbeatTimer != null) {
                sendHeartbeatTimer.destroy();
            }
            if (prop.isJoinCluster()) {
                boolean shutdown = false;
                for (Endpoint member : memberConfig.getMembersWithout(self.getSelf().getId())) {
                    RedirectReq req = RedirectReq.Builder.aRedirectReq()
                            .nodeId(self.getSelf().getId())
                            .redirect(RedirectReq.CHANGE_MEMBER)
                            .changeOp(Master.REMOVE)
                            .changeTarget(Sets.newHashSet(self.getSelf()))
                            .build();
                    RedirectRes changeRes = this.client.sendRequestSync(member, req, 2000);
                    if (changeRes != null && changeRes.isChangeResult()) {
                        shutdown = true;
                        break;
                    }
                }
                if (shutdown) {
                    throw new ShutdownException("shutdown, remove himself an exception occurs.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        waitHeartbeatTimer = new RepeatedTimer("follower-wait-heartbeat", prop.getPaxosProp().getMasterHeartbeatInterval()) {
            @Override
            protected void onTrigger() {
                memberConfig.changeMaster(PaxosMemberConfiguration.RESET_MASTER_ID);
                restartElect();
            }
        };

        electTimer = new RepeatedTimer("elect-master", calculateElectionMasterInterval()) {
            @Override
            protected void onTrigger() {
                election();
            }

            @Override
            protected int adjustTimeout(final int timeoutMs) {
                return calculateElectionMasterInterval();
            }
        };

        sendHeartbeatTimer = new RepeatedTimer("master-send-heartbeat", (int) (prop.getPaxosProp().getMasterHeartbeatInterval() * 0.95)) {
            @Override
            protected void onTrigger() {
                if (!sendHeartbeat(false)) {
                    restartElect();
                }
            }
        };
    }

    private int calculateElectionMasterInterval() {
        return ThreadLocalRandom.current().nextInt(prop.getPaxosProp().getMasterElectMinInterval(), prop.getPaxosProp().getMasterElectMaxInterval());
    }

    @Override
    public boolean isSelf() {
        return self.getSelf().equals(memberConfig.getMaster());
    }

    @Override
    public boolean changeMember(final byte op, final Set<Endpoint> target) {
        LOG.info("change member, op: {}, target: {}", op, target);

        if (RoleAccessor.getMaster().isSelf()) {
            return _changeMember(op, target);
        }

        Endpoint master = memberConfig.getMaster();
        if (master == null) {
            return false;
        }
        RedirectReq req = RedirectReq.Builder.aRedirectReq()
                .nodeId(self.getSelf().getId())
                .redirect(RedirectReq.CHANGE_MEMBER)
                .changeOp(op)
                .changeTarget(target)
                .build();
        RedirectRes res = client.sendRequestSync(master, req, prop.getRoundTimeout() * prop.getRetry());

        return res != null && res.isChangeResult();
    }

    /**
     * Change member by Join-Consensus.
     * e.g. old version = 0, new version = 1
     * 1. Accept phase → send new config to old quorum, enter Join-Consensus
     * 2. Copy instance(version = 0, confirmed) to new quorum
     * 3. Confirm phase → new config take effect
     *
     * @param op       add or remove member
     * @param endpoint target member
     * @return result
     */
    private boolean _changeMember(final byte op, final Set<Endpoint> endpoint) {

        try {
            // It can only be changed once at a time
            if (!changing.compareAndSet(false, true)) {
                return false;
            }

            PaxosMemberConfiguration curConfiguration = memberConfig.createRef();
            Set<Endpoint> newConfig = new HashSet<>(CollectionUtils.isEmpty(curConfiguration.getLastMembers())
                    ? curConfiguration.getEffectMembers()
                    : curConfiguration.getLastMembers());
            if (op == Master.ADD) {
                newConfig.addAll(endpoint);
            } else {
                endpoint.forEach(newConfig::remove);
            }

            // It only takes effect in the image
            int version = curConfiguration.seenNewConfig(newConfig);

            ChangeMemberOp req = new ChangeMemberOp();
            req.setNodeId(prop.getSelf().getId());
            req.setNewConfig(newConfig);
            req.setVersion(version);
            CompletableFuture<Boolean> latch = new CompletableFuture<>();

            RoleAccessor.getProposer().tryBoost(new Holder<Long>() {
                @Override
                protected Long create() {
                    return self.incrementInstanceId();
                }
            }, curConfiguration, Lists.newArrayList(
                    new ProposalWithDone(new Proposal(MasterSM.GROUP, req), new ProposeDone() {
                        @Override
                        public void negotiationDone(final boolean result, final boolean changed) {
                            if (!result || changed) {
                                latch.complete(false);
                                return;
                            }

                            // 2. Copy instance(version = 0, confirmed) to new quorum
                            pushCompleteData(newConfig, 0);
                        }

                        @Override
                        public void applyDone(final Proposal p, final Object r) {
                            latch.complete(true);
                        }
                    })
            ));
            return latch.get(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // do nothing
            return false;
        } finally {
            changing.compareAndSet(true, false);
        }
    }

    private void pushCompleteData(final Set<Endpoint> newConfig, final int times) {
        int cur = times + 1;
        if (cur >= 3) {
            throw new ChangeMemberException("push complete data to new quorum failure.");
        }

        Map<String, Snap> snaps = RoleAccessor.getLearner().generateSnap();
        List<Instance<Proposal>> instanceConfirmed = logManager.getInstanceConfirmed();
        PushCompleteDataReq completeDataReq = PushCompleteDataReq.Builder.aPushCompleteDataReq()
                .snaps(snaps)
                .confirmedInstances(instanceConfirmed)
                .build();

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Quorum quorum = new SingleQuorum(newConfig, this.nwr.w(newConfig.size()));
        quorum.grant(self.getSelf());
        if (quorum.isGranted() == Quorum.GrantResult.PASS) {
            future.complete(true);
        }
        newConfig.stream().filter(it -> !it.equals(self.getSelf()))
                .forEach(it -> client.sendRequestAsync(it, completeDataReq, new AbstractInvokeCallback<PushCompleteDataRes>() {
                    @Override
                    public void error(final Throwable err) {
                        quorum.refuse(it);
                        if (quorum.isGranted() == Quorum.GrantResult.REFUSE) {
                            future.complete(false);
                        }
                    }

                    @Override
                    public void complete(final PushCompleteDataRes result) {
                        if (result.isSuccess()) {
                            quorum.grant(it);
                        } else {
                            quorum.refuse(it);
                        }
                        if (quorum.isGranted() == Quorum.GrantResult.PASS) {
                            future.complete(true);
                        } else if (quorum.isGranted() == Quorum.GrantResult.REFUSE) {
                            future.complete(false);
                        }
                        // else ignore
                    }
                }, 2000));
        try {
            if (!future.get(2010, TimeUnit.MILLISECONDS)) {
                pushCompleteData(newConfig, cur);
            }
        } catch (Exception e) {
            throw new ConsensusException(String.format("push data to %s, %s", newConfig, e.getMessage()), e);
        }
    }

    @Override
    public void lookMaster() {
        PreElectReq req = PreElectReq.Builder.aPreElectReq()
                .memberConfigurationVersion(memberConfig.getVersion())
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .build();
        for (Endpoint it : memberConfig.getMembersWithout(self.getSelf().getId())) {
            PreElectRes res = client.sendRequestSync(it, req);
            if (res != null && res.getMaster() != null) {
                memberConfig.changeMaster(res.getMaster().getId());
                return;
            }
        }
        // there is no master in the cluster, do elect.
        restartElect();
    }

    @Override
    public void transferMaster() {

    }

    private void election() {

        if (!electing.compareAndSet(false, true)) {
            return;
        }

        try {
            LOG.info("start electing master.");
            ElectionOp req = new ElectionOp();
            req.setNodeId(self.getSelf().getId());

            CountDownLatch latch = new CountDownLatch(1);
            Proposal proposal = new Proposal(MasterSM.GROUP, req);
            RoleAccessor.getProposer().tryBoost(
                    new Holder<Long>() {
                        @Override
                        protected Long create() {
                            return self.incrementInstanceId();
                        }
                    }, Lists.newArrayList(new ProposalWithDone(proposal, (result, changed) -> {
                        LOG.info("electing master, negotiationDone: {}", result);
                        if (result && !changed) {
                            newMaster(latch);
                        } else {
                            latch.countDown();
                        }
                    })));

            try {
                boolean await = latch.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
                // do nothing for await's result, stop this timer in {@link #handleNewMasterRes}
            } catch (InterruptedException e) {
                LOG.debug(e.getMessage());
            }
        } finally {
            electing.compareAndSet(true, false);
        }
    }

    private void newMaster(final CountDownLatch latch) {
        LOG.info("start new master.");

        PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();
        NewMasterReq req = NewMasterReq.Builder.aNewMasterReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .build();
        Quorum quorum = QuorumFactory.createWriteQuorum(memberConfiguration);
        AtomicBoolean next = new AtomicBoolean(false);

        // for self
        NewMasterRes masterRes = onReceiveNewMaster(req, true);
        handleNewMasterRes(self.getSelf(), masterRes, quorum, next, latch);

        // for other members
        memberConfiguration.getMembersWithout(self.getSelf().getId()).forEach(it ->
                client.sendRequestAsync(it, req, new AbstractInvokeCallback<NewMasterRes>() {
                    @Override
                    public void error(final Throwable err) {
                        quorum.refuse(it);
                        if (quorum.isGranted() == SingleQuorum.GrantResult.REFUSE && next.compareAndSet(false, true)) {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void complete(final NewMasterRes result) {
                        handleNewMasterRes(it, result, quorum, next, latch);
                    }
                }, 50L));
    }

    private void handleNewMasterRes(final Endpoint it, final NewMasterRes result, final Quorum quorum, final AtomicBoolean next, final CountDownLatch latch) {
        self.updateCurInstanceId(result.getCurInstanceId());

        if (result.isGranted()) {
            quorum.grant(it);
            if (quorum.isGranted() == SingleQuorum.GrantResult.PASS && next.compareAndSet(false, true)) {
                memberConfig.changeMaster(self.getSelf().getId());
                ThreadExecutor.execute(MasterImpl.this::_boosting);
                restartSendHbNow();
                latch.countDown();
            }
        } else {
            quorum.refuse(it);
            if (quorum.isGranted() == SingleQuorum.GrantResult.REFUSE && next.compareAndSet(false, true)) {
                latch.countDown();
            }
        }
    }

    private void _boosting() {

        LOG.info("boosting instance.");
        long miniInstanceId = RoleAccessor.getLearner().getLastAppliedInstanceId();
        long maxInstanceId = self.getCurInstanceId();
        for (; miniInstanceId < maxInstanceId; miniInstanceId++) {
            Instance<Proposal> instance = logManager.getInstance(miniInstanceId);
            if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
                continue;
            }
            List<Proposal> grantedValue = instance != null && CollectionUtils.isNotEmpty(instance.getGrantedValue())
                    ? instance.getGrantedValue() : Lists.newArrayList(Proposal.NOOP);
            long finalMiniInstanceId = miniInstanceId;
            RoleAccessor.getProposer().tryBoost(new Holder<Long>() {
                @Override
                protected Long create() {
                    return finalMiniInstanceId;
                }
            }, grantedValue.stream().map(i -> new ProposalWithDone(i, new ProposeDone.FakeProposeDone())).collect(Collectors.toList()));
        }
    }

    /**
     * Send Heartbeat Msg.
     *
     * @param probe probe msg
     * @return true if the majority responds
     */
    private boolean sendHeartbeat(final boolean probe) {
        final long curInstanceId = self.getCurInstanceId();
        long lastCheckpoint = self.getLastCheckpoint();
        final PaxosMemberConfiguration memberConfiguration = memberConfig.createRef();

        final Quorum quorum = QuorumFactory.createWriteQuorum(memberConfiguration);
        final Ping req = Ping.Builder.aPing()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .memberConfigurationVersion(memberConfiguration.getVersion())
                .nodeState(NodeState.Builder.aNodeState()
                        .nodeId(self.getSelf().getId())
                        .maxInstanceId(curInstanceId)
                        .lastCheckpoint(lastCheckpoint)
                        .lastAppliedInstanceId(RoleAccessor.getLearner().getLastAppliedInstanceId())
                        .build())
                .timestampMs(TrueTime.currentTimeMillis())
                .probe(probe)
                .build();

        final CompletableFuture<SingleQuorum.GrantResult> complete = new CompletableFuture<>();
        // for self
        if (onReceiveHeartbeat(req, true)) {
            quorum.grant(self.getSelf());
            if (quorum.isGranted() == SingleQuorum.GrantResult.PASS) {
                complete.complete(quorum.isGranted());
            }
        }

        // for other members
        memberConfiguration.getMembersWithout(self.getSelf().getId()).forEach(it -> {
            client.sendRequestAsync(it, req, new AbstractInvokeCallback<Pong>() {
                @Override
                public void error(final Throwable err) {
                    LOG.debug("heartbeat, node: " + it.getId() + ", " + err.getMessage());
                    quorum.refuse(it);
                    if (quorum.isGranted() == SingleQuorum.GrantResult.REFUSE) {
                        complete.complete(SingleQuorum.GrantResult.REFUSE);
                    }
                }

                @Override
                public void complete(final Pong result) {
                    quorum.grant(it);
                    if (quorum.isGranted() == SingleQuorum.GrantResult.PASS) {
                        complete.complete(SingleQuorum.GrantResult.PASS);
                    }
                }
            });
        });
        try {
            SingleQuorum.GrantResult grantResult = complete.get(client.requestTimeout() + 10, TimeUnit.MILLISECONDS);
            return grantResult == SingleQuorum.GrantResult.PASS;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("master send heartbeat occur exception, {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean onReceiveHeartbeat(final Ping request, final boolean isSelf) {
        NodeState nodeState = request.getNodeState();

        self.updateCurInstanceId(nodeState.getMaxInstanceId());

        if (request.getMemberConfigurationVersion() >= memberConfig.getVersion()) {

            if (!request.isProbe() && !isSelf) {
                // check and update instance
                ThreadExecutor.execute(() -> {
                    RoleAccessor.getLearner().pullSameData(nodeState);
                });
            }

            TrueTime.heartbeat(request.getTimestampMs());

            // reset and restart election timer
            if (!isSelf) {
                restartWaitHb();
            }
            LOG.debug("receive heartbeat from node-{}, result: true.", request.getNodeId());
            return true;
        } else {
            LOG.debug("receive heartbeat from node-{}, result: false. local.master: {}, req.version: {}", request.getNodeId(),
                    memberConfig, request.getMemberConfigurationVersion());
            return false;
        }
    }

    @Override
    public NewMasterRes onReceiveNewMaster(final NewMasterReq request, final boolean isSelf) {
        if (request.getMemberConfigurationVersion() >= memberConfig.getVersion()) {
            memberConfig.seenCandidate(request.getNodeId());
            if (!isSelf) {
                memberConfig.changeMaster(request.getNodeId());
                restartWaitHb();
            }
            return NewMasterRes.Builder.aNewMasterRes()
                    .checkpoint(self.getLastCheckpoint())
                    .curInstanceId(self.getCurInstanceId())
                    .lastAppliedId(RoleAccessor.getLearner().getLastAppliedInstanceId())
                    .granted(true)
                    .build();
        } else {
            return NewMasterRes.Builder.aNewMasterRes()
                    .checkpoint(self.getLastCheckpoint())
                    .curInstanceId(self.getCurInstanceId())
                    .lastAppliedId(RoleAccessor.getLearner().getLastAppliedInstanceId())
                    .granted(false)
                    .build();
        }
    }

    private void restartWaitHb() {
        sendHeartbeatTimer.stop();
        waitHeartbeatTimer.stop();
        electTimer.stop();
        waitHeartbeatTimer.restart(false);
    }

    private void restartSendHbNow() {
        waitHeartbeatTimer.stop();
        sendHeartbeatTimer.stop();
        electTimer.stop();
        sendHeartbeatTimer.restart(true);
    }

    private void restartElect() {
        waitHeartbeatTimer.stop();
        sendHeartbeatTimer.stop();
        electTimer.stop();
        electTimer.restart(false);
    }

}
