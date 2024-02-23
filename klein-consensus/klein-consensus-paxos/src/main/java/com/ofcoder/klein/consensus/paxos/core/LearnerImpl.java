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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ofcoder.klein.common.Holder;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.facade.sm.SMApplier;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.NodeState;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Sync;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * learner implement.
 *
 * @author 释慧利
 */
public class LearnerImpl implements Learner {
    private static final Logger LOG = LoggerFactory.getLogger(LearnerImpl.class);
    private RpcClient client;
    private final PaxosNode self;
    private final PaxosMemberConfiguration memberConfig;
    private LogManager<Proposal> logManager;
    private final ConcurrentMap<String, SMApplier> sms = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, List<LearnCallback>> learningCallbacks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, List<ProposeDone>> applyCallback = new ConcurrentHashMap<>();
    private ConsensusProp prop;
    private Long lastAppliedId = 0L;
    private final Object appliedIdLock = new Object();
    private final AtomicBoolean learning = new AtomicBoolean(false);

    public LearnerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
    }

    @Override
    public void shutdown() {
        generateSnap();
        sms.values().forEach(SMApplier::close);
    }

    @Override
    public Map<String, Snap> generateSnap() {
        ConcurrentMap<String, Snap> result = new ConcurrentHashMap<>();
        // fixme: latch的数量根据sms来定，但是这中间sms可能会rm，那么latch就结束不了了
        CountDownLatch latch = new CountDownLatch(sms.size());
        sms.forEach((group, sm) -> {
            SMApplier.Task e = SMApplier.Task.createTakeSnapTask(new SMApplier.TaskCallback() {
                @Override
                public void onTakeSnap(final Snap snap) {
                    result.put(group, snap);
                    latch.countDown();
                }
            });

            sm.offer(e);
            // ignore offer result.
        });

        try {
            if (latch.await(1L, TimeUnit.SECONDS)) {
                return result;
            } else {
                LOG.error("generate snapshot timeout. succ: {}, all: {}", result.keySet(), sms.keySet());
                return result;
            }
        } catch (InterruptedException ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
            return result;
        }
    }

    @Override
    public void loadSnapSync(final Map<String, Snap> snaps) {
        if (MapUtils.isEmpty(snaps)) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(snaps.size());

        snaps.forEach((group, snap) -> {
            if (sms.containsKey(group)) {
                SMApplier.Task e = SMApplier.Task.createLoadSnapTask(snap, new SMApplier.TaskCallback() {
                    @Override
                    public void onLoadSnap(final long checkpoint) {
                        LOG.info("load snap success, group: {}, checkpoint: {}", group, checkpoint);

                        self.updateCurInstanceId(snap.getCheckpoint());
                        updateAppliedId(snap.getCheckpoint());

                        applyCallback.keySet().removeIf(it -> it <= checkpoint);
                        latch.countDown();

//                        replayLog(group, checkpoint);
                    }
                });
                sms.get(group).offer(e);
            } else {
                latch.countDown();
                LOG.warn("load snap failure, group: {}, The state machine is not found."
                        + " It may be that Klein is starting and the state machine has not been loaded. Or, the state machine is unloaded.", group);
            }
        });

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                LOG.error("load snapshot timeout. snaps: {}, all: {}", snaps.keySet(), sms.keySet());
            }
        } catch (InterruptedException ex) {
            LOG.error(String.format("load snapshot occur exception. %s", ex.getMessage()), ex);
        }
    }

    @Override
    public void replayLog(final String group, final long start) {
        if (!sms.containsKey(group)) {
            LOG.error("{} replay log, but the sm is not exists.", group);
            return;
        }
        SMApplier applier = sms.get(group);

        long curInstanceId = self.getCurInstanceId();
        for (long i = start; i <= curInstanceId; i++) {
            Instance<Proposal> instance = logManager.getInstance(i);
            if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
                break;
            }
            List<Command> replayData = instance.getGrantedValue().stream()
                    .filter(it -> StringUtils.equals(group, it.getGroup()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(replayData)) {
                continue;
            }

            SMApplier.Task e = SMApplier.Task.createReplayTask(i, replayData);

            applier.offer(e);
        }
    }

    @Override
    public long getLastAppliedInstanceId() {
        return lastAppliedId;
    }

    @Override
    public long getLastCheckpoint() {
        return sms.values().stream()
                .mapToLong(SMApplier::getLastCheckpoint)
                .max().orElse(-1L);
    }

    private void updateAppliedId(final long instanceId) {
        if (lastAppliedId < instanceId) {
            synchronized (appliedIdLock) {
                this.lastAppliedId = Math.max(lastAppliedId, instanceId);
            }
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, new SMApplier(group, sm)) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        if (lastSnap == null) {
            return;
        }
        loadSnapSync(ImmutableMap.of(group, lastSnap));
    }

    @Override
    public void alignData(final NodeState state) {
        final Endpoint target = memberConfig.getEndpointById(state.getNodeId());
        if (target == null) {
            return;
        }
        final long targetCheckpoint = state.getLastCheckpoint();
        final long targetApplied = state.getLastAppliedInstanceId();
        long localApplied = lastAppliedId;
        long localCheckpoint = getLastCheckpoint();
        if (targetApplied <= localApplied) {
            // same data
            return;
        }

        LOG.info("keepSameData, target[id: {}, cp: {}, maxAppliedInstanceId:{}], local[cp: {}, maxAppliedInstanceId:{}]",
                target.getId(), targetCheckpoint, targetApplied, getLastCheckpoint(), localApplied);
        if (targetCheckpoint > localApplied || targetCheckpoint - localCheckpoint >= 100) {
            snapSync(target);
            alignData(state);
        } else {
            for (long i = localApplied + 1; i <= targetApplied; i++) {
                learn(i, target, new DefaultLearnCallback());
            }
        }
    }

    /**
     * Send the learn message to <code>target</code>.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @param callback   Callbacks of learning results
     */
    private void learn(final long instanceId, final Endpoint target, final LearnCallback callback) {
        learningCallbacks.putIfAbsent(instanceId, new ArrayList<>());
        learningCallbacks.get(instanceId).add(callback);

        Instance<Proposal> instance = logManager.getInstance(instanceId);
        if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
            List<LearnCallback> remove = learningCallbacks.remove(instanceId);
            if (remove != null) {
                remove.forEach(it -> it.learned(true));
            }
            return;
        }

        LOG.info("start learn instanceId[{}] from node-{}", instanceId, target.getId());
        LearnReq req = LearnReq.Builder.aLearnReq().instanceId(instanceId).nodeId(self.getSelf().getId()).build();
        client.sendRequestAsync(target, req, new AbstractInvokeCallback<LearnRes>() {
            @Override
            public void error(final Throwable err) {
                LOG.error("learn instance[{}] from node-{}, {}", instanceId, target.getId(), err.getMessage());
                List<LearnCallback> remove = learningCallbacks.remove(instanceId);
                if (remove != null) {
                    remove.forEach(it -> it.learned(false));
                }
            }

            @Override
            public void complete(final LearnRes result) {
                if (result.isResult() == Sync.SNAP) {
                    LOG.info("learn instance[{}] from node-{}, sync.type: SNAP", instanceId, target.getId());
                    long checkpoint = snapSync(target);
                    Set<Long> learned = learningCallbacks.keySet().stream().filter(it -> checkpoint >= it).collect(Collectors.toSet());
                    learned.forEach(it -> {
                        List<LearnCallback> remove = learningCallbacks.remove(it);
                        if (remove != null) {
                            remove.forEach(r -> r.learned(true));
                        }
                    });
                } else if (result.isResult() == Sync.SINGLE) {
                    LOG.info("learn instance[{}] from node-{}, sync.type: SINGLE", instanceId, target.getId());
                    Instance<Proposal> update = Instance.Builder.<Proposal>anInstance()
                            .instanceId(instanceId)
                            .proposalNo(result.getInstance().getProposalNo())
                            .grantedValue(result.getInstance().getGrantedValue())
                            .state(result.getInstance().getState())
                            .build();

                    try {
                        logManager.getLock().writeLock().lock();
                        logManager.updateInstance(update);
                    } finally {
                        logManager.getLock().writeLock().unlock();
                    }
                    // apply statemachine
                    apply(req.getInstanceId());

                    List<LearnCallback> remove = learningCallbacks.remove(instanceId);
                    if (remove != null) {
                        remove.forEach(r -> r.learned(true));
                    }
                } else {
                    LOG.info("learn instance[{}] from node-{}, sync.type: NO_SUPPORT", instanceId, target.getId());
                    List<LearnCallback> remove = learningCallbacks.remove(instanceId);
                    if (remove != null) {
                        remove.forEach(r -> r.learned(false));
                    }
                }
            }
        }, 1000);
    }

    private long snapSync(final Endpoint target) {
        LOG.info("start snap sync from node-{}", target.getId());

        long checkpoint = -1;
        try {
            SnapSyncReq req = SnapSyncReq.Builder.aSnapSyncReq()
                    .nodeId(self.getSelf().getId())
                    .proposalNo(self.getCurProposalNo())
                    .memberConfigurationVersion(memberConfig.getVersion())
                    .checkpoint(getLastCheckpoint())
                    .build();
            SnapSyncRes res = client.sendRequestSync(target, req, 1000);

            if (res != null) {
                //
                loadSnapSync(res.getImages());
                checkpoint = res.getImages().values().stream().max(Comparator.comparingLong(Snap::getCheckpoint)).orElse(new Snap(checkpoint, null)).getCheckpoint();
            }

            return checkpoint;
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            return checkpoint;
        }
    }

    @Override
    public void confirm(final long instanceId, final String checksum, final List<ProposalWithDone> dons) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);
        applyCallback.putIfAbsent(instanceId, dons.stream().map(ProposalWithDone::getDone).collect(Collectors.toList()));

        // A proposalNo here does not have to use the proposalNo of the accept phase,
        // because the proposal is already in the confirm phase and it will not change.
        // Instead, using self.proposalNo allows you to more quickly advance a proposalNo for another member
        long curProposalNo = self.getCurProposalNo();

        PaxosMemberConfiguration configuration = memberConfig.createRef();

        ConfirmReq req = ConfirmReq.Builder.aConfirmReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(curProposalNo)
                .instanceId(instanceId)
                .checksum(checksum)
                .build();

        // for self
        if (handleConfirmRequest(req)) {
            // apply statemachine
            apply(instanceId);
        } else {
            dons.forEach(it -> it.getDone().applyDone(new HashMap<>()));
        }

        // for other members
        configuration.getMembersWithout(self.getSelf().getId())
                .forEach(it -> client.sendRequestAsync(it, req, new AbstractInvokeCallback<Serializable>() {
                    @Override
                    public void error(final Throwable err) {
                        LOG.error("send confirm msg to node-{}, instance[{}], {}", it.getId(), instanceId, err.getMessage());
                        // do nothing
                    }

                    @Override
                    public void complete(final Serializable result) {
                        // do nothing
                    }
                }));
    }

    private boolean handleConfirmRequest(final ConfirmReq req) {
        LOG.info("processing the confirm message from node-{}, instance: {}", req.getNodeId(), req.getInstanceId());

        if (req.getInstanceId() <= getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), getLastCheckpoint());
            return false;
        }

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null || localInstance.getState() == Instance.State.PREPARED) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new DefaultLearnCallback());
                return false;
            }

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                // the instance is confirmed.
                LOG.info("the instance[{}] is confirmed", localInstance.getInstanceId());
                return false;
            }

            // check sum
            if (!StringUtils.equals(localInstance.getChecksum(), req.getChecksum())) {
                LOG.info("local.checksum: {}, req.checksum: {}", localInstance.getChecksum(), req.getChecksum());
                learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()), new DefaultLearnCallback());
                return false;
            }

            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setProposalNo(req.getProposalNo());
            logManager.updateInstance(localInstance);

            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            self.updateCurInstanceId(req.getInstanceId());
            logManager.getLock().writeLock().unlock();
        }
    }

    @Override
    public void handleConfirmRequest(final ConfirmReq req, final boolean isSelf) {
        if (handleConfirmRequest(req)) {
            // apply statemachine
            apply(req.getInstanceId());
        }
    }

    @Override
    public LearnRes handleLearnRequest(final LearnReq request) {
        LOG.info("received a learn message from node[{}] about instance[{}]", request.getNodeId(), request.getInstanceId());
        LearnRes.Builder res = LearnRes.Builder.aLearnRes().nodeId(self.getSelf().getId());

        if (request.getInstanceId() <= getLastCheckpoint()) {
            return res.result(Sync.SNAP).build();
        }

        Instance<Proposal> instance = logManager.getInstance(request.getInstanceId());

        if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
            LOG.debug("NO_SUPPORT, learnInstance[{}], cp: {}, cur: {}", request.getInstanceId(),
                    getLastCheckpoint(), self.getCurInstanceId());
            if (memberConfig.allowBoost()) {
                LOG.debug("NO_SUPPORT, but i am master, try boost: {}", request.getInstanceId());

                List<Proposal> defaultValue = instance == null || CollectionUtils.isEmpty(instance.getGrantedValue())
                        ? Lists.newArrayList(Proposal.NOOP) : instance.getGrantedValue();
                CountDownLatch latch = new CountDownLatch(1);

                RuntimeAccessor.getProposer().tryBoost(new Holder<Long>() {
                    @Override
                    protected Long create() {
                        return request.getInstanceId();
                    }
                }, defaultValue.stream().map(it -> new ProposalWithDone(it, (result, dataChange) -> latch.countDown())).collect(Collectors.toList()));

                try {
                    boolean await = latch.await(this.prop.getRoundTimeout() * this.prop.getRetry(), TimeUnit.MILLISECONDS);
                    // do nothing for await's result
                } catch (InterruptedException e) {
                    LOG.debug(e.getMessage());
                }

                instance = logManager.getInstance(request.getInstanceId());
            }
        }
        if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
            return res.result(Sync.NO_SUPPORT).instance(instance).build();
        } else {
            return res.result(Sync.SINGLE).instance(instance).build();
        }
    }

    @Override
    public SnapSyncRes handleSnapSyncRequest(final SnapSyncReq req) {
        LOG.info("processing the pull snap message from node-{}", req.getNodeId());
        SnapSyncRes res = SnapSyncRes.Builder.aSnapSyncRes()
                .images(new HashMap<>())
                .checkpoint(getLastCheckpoint())
                .build();
        for (String group : sms.keySet()) {
            Snap lastSnap = logManager.getLastSnap(group);
            if (lastSnap != null && lastSnap.getCheckpoint() > req.getCheckpoint()) {
                res.getImages().put(group, lastSnap);
            }
        }
        return res;
    }

    private void apply(final long instanceId) {

        final long lastCheckpoint = getLastCheckpoint();
        final long lastApplyId = Math.max(lastAppliedId, lastCheckpoint);
        final long expectConfirmId = lastApplyId + 1;
        LOG.info("start apply, instanceId: {}, curAppliedInstanceId: {}, lastCheckpoint: {}", instanceId, lastAppliedId, lastCheckpoint);

        if (instanceId <= lastApplyId) {
            // the instance has been applied.
            return;
        }

        if (instanceId > expectConfirmId) {
            // boost.
            for (long i = expectConfirmId; i < instanceId; i++) {
                Instance<Proposal> exceptInstance = logManager.getInstance(i);
                if (exceptInstance != null && exceptInstance.getState() == Instance.State.CONFIRMED) {
                    _apply(i);
                } else {
                    List<Proposal> defaultValue = exceptInstance == null || CollectionUtils.isEmpty(exceptInstance.getGrantedValue())
                            ? Lists.newArrayList(Proposal.NOOP) : exceptInstance.getGrantedValue();
                    learnHard(i, defaultValue);
                }
            }
            return;
        }
        // else: instanceId == exceptConfirmId, do apply.

        _apply(instanceId);
    }

    private void _apply(final long instanceId) {
        Instance<Proposal> localInstance = logManager.getInstance(instanceId);
        Map<String, List<Command>> groupProposals = localInstance.getGrantedValue().stream()
                .filter(it -> it != Proposal.NOOP)
                .collect(Collectors.groupingBy(Proposal::getGroup, Collectors.toList()));

        groupProposals.forEach((group, proposals) -> {
            if (sms.containsKey(group)) {
                SMApplier.Task task = SMApplier.Task.createApplyTask(instanceId, proposals, new SMApplier.TaskCallback() {

                    @Override
                    public void onApply(final Map<Command, Object> result) {
                        List<ProposeDone> remove = applyCallback.remove(instanceId);
                        if (remove != null) {
                            Map<Proposal, Object> proposalResult = new HashMap<>();
                            result.forEach((k, v) -> proposalResult.put((Proposal) k, v));
                            remove.forEach(it -> it.applyDone(proposalResult));
                        }
                        updateAppliedId(instanceId);
                    }
                });

                sms.get(group).offer(task);
            } else {
                LOG.info("offer apply task, the {} sm is not exists. instanceId: {}", group, instanceId);
            }
        });
    }

    private boolean learnHard(final long instanceId, final List<Proposal> defaultValue) {
        boolean lr = false;

        if (memberConfig.allowBoost()) {
            LOG.info("try boost instance: {}", instanceId);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            RuntimeAccessor.getProposer().tryBoost(
                    new Holder<Long>() {
                        @Override
                        protected Long create() {
                            return instanceId;
                        }

                    }, defaultValue.stream().map(it -> new ProposalWithDone(it, (result, dataChange) -> future.complete(result)))
                            .collect(Collectors.toList())
            );
            try {
                lr = future.get(prop.getRoundTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // do nothing, lr = false
            }
        } else {
            final Endpoint master = memberConfig.getMaster();
            if (master != null) {
                lr = learnSync(instanceId, master);
            } else {
                LOG.info("learnHard, instance: {}, master is null, wait master.", instanceId);
                // learn hard
//                for (Endpoint it : self.getMemberConfiguration().getMembersWithoutSelf()) {
//                    lr = learnSync(instanceId, it);
//                    if (lr) {
//                        break;
//                    }
//                }
            }
        }
        return lr;
    }

    /**
     * Synchronous learning.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @return learn result
     */
    private boolean learnSync(long instanceId, Endpoint target) {
        long singleTimeoutMS = 150;
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        learn(instanceId, target, future::complete);
        try {
            return future.get(singleTimeoutMS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }

}
