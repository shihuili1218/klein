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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    private final ConcurrentMap<String, SMApplier<Proposal>> sms = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, List<LearnCallback>> learningCallbacks = new ConcurrentHashMap<>();
    private ConsensusProp prop;
    private Long lastAppliedId = 0L;
    private final Object appliedIdLock = new Object();
    private final SMApplier.TaskCallback<Proposal> fakeCallback = new SMApplier.TaskCallback<Proposal>() {
    };
    private final ConcurrentMap<Long, SMApplier.TaskCallback<Proposal>> applyCallback = new ConcurrentHashMap<>();

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
            Snap lastSnap = logManager.getLastSnap(group);

            if (lastSnap != null && lastSnap.getCheckpoint() >= sm.getLastAppliedId()) {
                result.put(group, lastSnap);
                latch.countDown();
            } else {
                SMApplier.Task<Proposal> e = SMApplier.Task.createTakeSnapTask(new SMApplier.TaskCallback<Proposal>() {
                    @Override
                    public void onTakeSnap(final Snap snap) {
                        result.put(group, snap);
                        logManager.saveSnap(group, snap);

                        latch.countDown();
                    }
                });

                sm.offer(e);
                // ignore offer result.
            }
        });

        Optional<Snap> max = result.values().stream().max(Comparator.comparingLong(Snap::getCheckpoint));
        max.ifPresent(snap -> self.updateLastCheckpoint(snap.getCheckpoint()));

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
    public void loadSnap(final Map<String, Snap> snaps) {
        if (MapUtils.isEmpty(snaps)) {
            return;
        }

        snaps.forEach((group, snap) -> {
            Snap localSnap = logManager.getLastSnap(group);
            if (localSnap != null && localSnap.getCheckpoint() > snap.getCheckpoint()) {
                LOG.warn("load snap skip, group: {}, local.checkpoint: {}, snap.checkpoint: {}", group, localSnap.getCheckpoint(), snap.getCheckpoint());
                return;
            }
            LOG.info("load snap, group: {}, checkpoint: {}", group, snap.getCheckpoint());

            if (sms.containsKey(group)) {
                SMApplier.Task<Proposal> e = SMApplier.Task.createLoadSnapTask(snap, new SMApplier.TaskCallback<Proposal>() {
                    @Override
                    public void onLoadSnap(long checkpoint) {
                        LOG.info("load snap success, group: {}, checkpoint: {}", group, checkpoint);
                        self.updateLastCheckpoint(checkpoint);
                        self.updateCurInstanceId(checkpoint);
                    }
                });
                boolean offer = sms.get(group).offer(e);
                // ignore offer result.
            } else {
                LOG.warn("load snap failure, group: {}, The state machine is not found."
                        + " It may be that Klein is starting and the state machine has not been loaded. Or, the state machine is unloaded.", group);

            }
        });
    }

    @Override
    public void replayLog(final String group, final long start) {
        if (!sms.containsKey(group)) {
            LOG.error("{} replay log, but the sm is not exists.", group);
            return;
        }
        SMApplier<Proposal> applier = sms.get(group);

        long curInstanceId = self.getCurInstanceId();
        for (long i = start; i <= curInstanceId; i++) {
            Instance<Proposal> instance = logManager.getInstance(i);
            if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
                break;
            }
            List<Proposal> replayData = instance.getGrantedValue().stream().filter(it -> StringUtils.equals(group, it.getGroup())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(replayData)) {
                continue;
            }

            SMApplier.Task<Proposal> e = SMApplier.Task.createReplayTask(i, replayData, fakeCallback);

            boolean offer = applier.offer(e);
            // ignore offer result.
        }
    }

    @Override
    public long getLastAppliedInstanceId() {
        return lastAppliedId;
    }

    // todo
    private void updateAppliedId(final long instanceId) {
        if (lastAppliedId < instanceId) {
            synchronized (appliedIdLock) {
                this.lastAppliedId = Math.max(lastAppliedId, instanceId);
            }
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, new SMApplier<>(group, sm)) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        if (lastSnap == null) {
            return;
        }
        loadSnap(ImmutableMap.of(group, lastSnap));
    }

    @Override
    public void learn(final long instanceId, final Endpoint target, final LearnCallback callback) {
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
                    if (!learned.contains(instanceId)) {
                        List<LearnCallback> remove = learningCallbacks.remove(instanceId);
                        if (remove != null) {
                            remove.forEach(r -> r.learned(false));
                        }
                    }
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
                    doApply(req.getInstanceId(), fakeCallback);

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

    @Override
    public void pullSameData(final NodeState state) {
        final Endpoint target = memberConfig.getEndpointById(state.getNodeId());
        if (target == null) {
            return;
        }
        final long targetCheckpoint = state.getLastCheckpoint();
        final long targetApplied = state.getLastAppliedInstanceId();
        long localApplied = lastAppliedId;
        long localCheckpoint = self.getLastCheckpoint();
        if (targetApplied <= localApplied) {
            // same data
            return;
        }

        LOG.info("keepSameData, target[id: {}, cp: {}, maxAppliedInstanceId:{}], local[cp: {}, maxAppliedInstanceId:{}]",
                target.getId(), targetCheckpoint, targetApplied, self.getLastCheckpoint(), localApplied);
        if (targetCheckpoint > localApplied || targetCheckpoint - localCheckpoint >= 100) {
            snapSync(target);
            pullSameData(state);
        } else {
            for (long i = localApplied + 1; i <= targetApplied; i++) {
                RuntimeAccessor.getLearner().learn(i, target);
            }
        }
    }

    @Override
    public void pushSameData(final Endpoint state) {

    }

    @Override
    public boolean healthy() {
        return true;
    }

    private long snapSync(final Endpoint target) {
        LOG.info("start snap sync from node-{}", target.getId());

        long checkpoint = -1;
        try {
            SnapSyncReq req = SnapSyncReq.Builder.aSnapSyncReq()
                    .nodeId(self.getSelf().getId())
                    .proposalNo(self.getCurProposalNo())
                    .memberConfigurationVersion(memberConfig.getVersion())
                    .checkpoint(self.getLastCheckpoint())
                    .build();
            SnapSyncRes res = client.sendRequestSync(target, req, 1000);

            if (res != null) {
                loadSnap(res.getImages());
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
            doApply(instanceId, new SMApplier.TaskCallback<Proposal>() {
                @Override
                public void onApply(final Map<Proposal, Object> result) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("instance[{}] applied, result: {}", instanceId, result);
                    }
                    dons.forEach(it -> it.getDone().applyDone(it.getProposal(), result.get(it.getProposal())));
                }
            });
        } else {
            dons.forEach(it -> it.getDone().applyDone(null, null));
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

        if (req.getInstanceId() <= self.getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), self.getLastCheckpoint());
            return false;
        }

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null || localInstance.getState() == Instance.State.PREPARED) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()));
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
                learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()));
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
            doApply(req.getInstanceId(), fakeCallback);
        }
    }

    @Override
    public LearnRes handleLearnRequest(final LearnReq request) {
        LOG.info("received a learn message from node[{}] about instance[{}]", request.getNodeId(), request.getInstanceId());
        LearnRes.Builder res = LearnRes.Builder.aLearnRes().nodeId(self.getSelf().getId());

        if (request.getInstanceId() <= self.getLastCheckpoint()) {
            return res.result(Sync.SNAP).build();
        }

        Instance<Proposal> instance = logManager.getInstance(request.getInstanceId());

        if (instance == null || instance.getState() != Instance.State.CONFIRMED) {
            LOG.debug("NO_SUPPORT, learnInstance[{}], cp: {}, cur: {}", request.getInstanceId(),
                    self.getLastCheckpoint(), self.getCurInstanceId());
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
                .checkpoint(self.getLastCheckpoint())
                .build();
        for (String group : sms.keySet()) {
            Snap lastSnap = logManager.getLastSnap(group);
            if (lastSnap != null && lastSnap.getCheckpoint() > req.getCheckpoint()) {
                res.getImages().put(group, lastSnap);
            }
        }
        return res;
    }

    private void doApply(long instanceId, SMApplier.TaskCallback<Proposal> callback) {
        applyCallback.putIfAbsent(instanceId, callback);

        final long lastCheckpoint = self.getLastCheckpoint();
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
                    // todo: 这是啥意思？
//                    applyQueue.offer(new SMApplier.Task(i, SMApplier.TaskEnum.APPLY, fakeCallback));
                } else {
                    List<Proposal> defaultValue = exceptInstance == null || CollectionUtils.isEmpty(exceptInstance.getGrantedValue())
                            ? Lists.newArrayList(Proposal.NOOP) : exceptInstance.getGrantedValue();
                    learnHard(i, defaultValue);
                }
            }
            // todo：如果不offer，当前instanceId应该什么时候执行？
//            applyQueue.offer(task);
            return;
        }
        // else: instanceId == exceptConfirmId, do apply.

        Instance<Proposal> localInstance = logManager.getInstance(instanceId);
        Map<String, List<Proposal>> groupProposals = localInstance.getGrantedValue().stream()
                .filter(it -> it != Proposal.NOOP)
                .collect(Collectors.groupingBy(Proposal::getGroup));
        groupProposals.forEach((group, proposals) -> {
            if (sms.containsKey(group)) {
                SMApplier.Task<Proposal> task = SMApplier.Task.createApplyTask(instanceId, proposals, callback);

                if (!sms.get(group).offer(task)) {
                    LOG.error("failed to push the instance[{}] to the applyQueue.", instanceId);
                    // do nothing, other threads will boost the instance
                }
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

}
