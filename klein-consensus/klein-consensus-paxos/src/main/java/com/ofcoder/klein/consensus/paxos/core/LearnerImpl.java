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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
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
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
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
    private final ConcurrentMap<String, SM> sms = new ConcurrentHashMap<>();
    private final BlockingQueue<Task> applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(value -> value.priority));
    private final ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create("apply-instance", true));
    private final ConcurrentMap<Long, List<LearnCallback>> learningCallbacks = new ConcurrentHashMap<>();
    private ConsensusProp prop;
    private Long lastAppliedId = 0L;
    private final Object appliedIdLock = new Object();
    private final TaskCallback fakeCallback = new TaskCallback() {
    };

    public LearnerImpl(final PaxosNode self) {
        this.self = self;
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
    }

    @Override
    public void init(final ConsensusProp op) {
        this.prop = op;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();

        applyExecutor.execute(() -> {
            while (true) {
                try {
                    Task take = applyQueue.take();
                    switch (take.taskType) {
                        case APPLY:
                            _apply(take);
                            break;
                        case REPLAY:
                            _replay(take);
                            break;
                        case SNAP_LOAD:
                            _loadSnap(take);
                            break;
                        case SNAP_TAKE:
                            _generateSnap(take);
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException e) {
                    LOG.warn("handle queue task, occur exception, {}", e.getMessage());
                }
            }
        });
    }

    @Override
    public void shutdown() {
        generateSnap();
        sms.values().forEach(SM::close);
    }

    private void _generateSnap(final Task task) {
        Map<String, Snap> snaps = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(sms.size());
        sms.forEach((group, sm) -> ThreadExecutor.execute(() -> {
            try {
                Snap lastSnap = logManager.getLastSnap(group);
                if (lastSnap != null && lastSnap.getCheckpoint() >= sm.lastAppliedId()) {
                    snaps.put(group, lastSnap);
                    return;
                }

                Snap snapshot = sm.snapshot();
                LOG.info("save snapshot, group: {}, cp: {}", group, snapshot.getCheckpoint());
                self.updateLastCheckpoint(snapshot.getCheckpoint());
                logManager.saveSnap(group, snapshot);
                snaps.put(group, snapshot);
            } finally {
                latch.countDown();
            }
        }));

        try {
            if (!latch.await(2L, TimeUnit.SECONDS)) {
                LOG.warn("generate snapshot, after waiting for 2 seconds, still didn't get all result. finish: {}", snaps.keySet());
            }
        } catch (InterruptedException e) {
            LOG.warn("generate snapshot, {}, {}", snaps.keySet(), e.getMessage());
        }
        task.callback.onTakeSnap(snaps);
    }

    @Override
    public Map<String, Snap> generateSnap() {
        CompletableFuture<Map<String, Snap>> future = new CompletableFuture<>();
        Task e = new Task(Task.HIGH_PRIORITY, TaskEnum.SNAP_TAKE, new TaskCallback() {
            @Override
            public void onTakeSnap(final Map<String, Snap> snaps) {
                future.complete(snaps);
            }
        });
        boolean offer = applyQueue.offer(e);
        // ignore offer result.
        try {
            return future.get(1L, TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
            return null;
        }
    }

    private void _loadSnap(final Task task) {
        CountDownLatch latch = new CountDownLatch(task.loadSnaps.size());

        task.loadSnaps.forEach((group, snap) -> ThreadExecutor.execute(() -> {
            try {
                Snap localSnap = logManager.getLastSnap(group);
                if (localSnap != null && localSnap.getCheckpoint() > snap.getCheckpoint()) {
                    return;
                }

                LOG.info("load snap, group: {}, checkpoint: {}", group, snap.getCheckpoint());

                SM sm = sms.get(group);
                if (sm == null) {
                    LOG.warn("load snap failure, group: {}, The state machine is not found."
                            + " It may be that Klein is starting and the state machine has not been loaded. Or, the state machine is unloaded.", group);
                    return;
                }

                sm.loadSnap(snap);
                self.updateLastCheckpoint(snap.getCheckpoint());
                self.updateCurInstanceId(snap.getCheckpoint());
                updateAppliedId(snap.getCheckpoint());

                replayLog(group, snap.getCheckpoint());

                LOG.info("load snap success, group: {}, checkpoint: {}", group, snap.getCheckpoint());
            } finally {
                latch.countDown();
            }
        }));

        try {
            boolean await = latch.await(2L, TimeUnit.SECONDS);
            // do nothing for await.result.
        } catch (InterruptedException e) {
            LOG.warn("load snap, {}", e.getMessage());
        }
    }

    @Override
    public void loadSnap(final Map<String, Snap> snaps) {
        if (MapUtils.isEmpty(snaps)) {
            return;
        }

        Task e = new Task(Task.HIGH_PRIORITY, TaskEnum.SNAP_LOAD, fakeCallback);
        e.loadSnaps = snaps;
        boolean offer = applyQueue.offer(e);
        // ignore offer result.
    }

    private void _replay(final Task task) {
        Map<Proposal, Object> result = this.doApply(task.priority, task.replayProposals);
        task.callback.onApply(result);
    }

    @Override
    public void replayLog(final String group, final long start) {
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

            Task e = new Task(i, TaskEnum.REPLAY, fakeCallback);
            e.replayProposals = replayData;
            boolean offer = applyQueue.offer(e);
            // ignore offer result.
        }
    }

    @Override
    public long getLastAppliedInstanceId() {
        return lastAppliedId;
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
        if (sms.putIfAbsent(group, sm) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        if (lastSnap == null) {
            return;
        }
        loadSnap(ImmutableMap.of(group, lastSnap));
    }

    private void _apply(final Task task) {
        final long lastCheckpoint = self.getLastCheckpoint();
        final long lastApplyId = Math.max(lastAppliedId, lastCheckpoint);
        final long exceptConfirmId = lastApplyId + 1;
        final long instanceId = task.priority;
        LOG.info("start apply, instanceId: {}, curAppliedInstanceId: {}, lastCheckpoint: {}", instanceId, lastAppliedId, lastCheckpoint);

        if (instanceId <= lastApplyId) {
            // the instance has been applied.
            return;
        }
        if (instanceId > exceptConfirmId) {
            // boost.
            for (long i = exceptConfirmId; i < instanceId; i++) {
                Instance<Proposal> exceptInstance = logManager.getInstance(i);
                if (exceptInstance != null && exceptInstance.getState() == Instance.State.CONFIRMED) {
                    applyQueue.offer(new Task(i, TaskEnum.APPLY, fakeCallback));
                } else {
                    List<Proposal> defaultValue = exceptInstance == null || CollectionUtils.isEmpty(exceptInstance.getGrantedValue())
                            ? Lists.newArrayList(Proposal.NOOP) : exceptInstance.getGrantedValue();
                    learnHard(i, defaultValue);
                }
            }

            applyQueue.offer(task);
            return;
        }
        // else: instanceId == exceptConfirmId, do apply.

        Instance<Proposal> localInstance = logManager.getInstance(instanceId);

        Map<Proposal, Object> applyResult = this.doApply(localInstance.getInstanceId(), localInstance.getGrantedValue());
        updateAppliedId(instanceId);

        task.callback.onApply(applyResult);
    }

    private Map<Proposal, Object> doApply(final long instance, final List<Proposal> datas) {
        Map<Proposal, Object> result = new HashMap<>();

        if (instance <= self.getLastCheckpoint()) {
            //do nothing
            return result;
        }
        LOG.debug("doing apply instance[{}]", instance);

        for (Proposal data : datas) {
            if (data.getData() instanceof Proposal.Noop) {
                result.put(data, null);
                continue;
            }
            if (sms.containsKey(data.getGroup())) {
                SM sm = sms.get(data.getGroup());
                try {
                    result.put(data, sm.apply(instance, data.getData()));
                } catch (Exception e) {
                    LOG.error(String.format("apply instance[%s].[%s] to sm, %s", instance, data, e.getMessage()), e);
                    result.put(data, null);
                }
            } else {
                LOG.error("the group[{}] is not loaded with sm, and the instance[{}] is not applied", data.getGroup(), instance);
                result.put(data, null);
            }
        }
        return result;
    }

    private boolean learnHard(final long instanceId, final List<Proposal> defaultValue) {
        boolean lr = false;

        if (Master.ElectState.allowBoost(RoleAccessor.getMaster().electState())) {
            LOG.info("try boost instance: {}", instanceId);
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            RoleAccessor.getProposer().tryBoost(
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
                    if (!applyQueue.offer(new Task(req.getInstanceId(), TaskEnum.APPLY, fakeCallback))) {
                        LOG.error("failed to push the instance[{}] to the applyQueue, applyQueue.size = {}.", req.getInstanceId(), applyQueue.size());
                        // do nothing, other threads will boost the instance
                    }
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
                RoleAccessor.getLearner().learn(i, target);
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
    public void confirm(final long instanceId, final List<ProposalWithDone> dons) {
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
                .build();

        // for self
        handleConfirmRequest(req, new TaskCallback() {
            @Override
            public void onApply(final Map<Proposal, Object> result) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("instance[{}] applied, result: {}", instanceId, result);
                }
                dons.forEach(it -> it.getDone().applyDone(it.getProposal(), result.get(it.getProposal())));
            }
        });

        // for other members
        configuration.getMembersWithout(self.getSelf().getId()).forEach(it -> client.sendRequestAsync(it, req, new AbstractInvokeCallback<Serializable>() {
            @Override
            public void error(final Throwable err) {
                LOG.error("send confirm msg to node-{}, instance[{}], {}", it.getId(), instanceId, err.getMessage());
                // do nothing
            }

            @Override
            public void complete(final Serializable result) {
                // do nothing
            }
        }, 1000));
    }

    private void handleConfirmRequest(final ConfirmReq req, final TaskCallback dons) {
        LOG.info("processing the confirm message from node-{}, instance: {}", req.getNodeId(), req.getInstanceId());

        if (req.getInstanceId() <= self.getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), self.getLastCheckpoint());
            return;
        }

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null || localInstance.getState() == Instance.State.PREPARED) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                learn(req.getInstanceId(), memberConfig.getEndpointById(req.getNodeId()));
                return;
            }

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                // the instance is confirmed.
                LOG.info("the instance[{}] is confirmed", localInstance.getInstanceId());
                return;
            }

            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setProposalNo(req.getProposalNo());
            logManager.updateInstance(localInstance);

            // apply statemachine
            if (!applyQueue.offer(new Task(req.getInstanceId(), TaskEnum.APPLY, dons))) {
                LOG.error("failed to push the instance[{}] to the applyQueue, applyQueue.size = {}.", req.getInstanceId(), applyQueue.size());
                // do nothing, other threads will boost the instance
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            self.updateCurInstanceId(req.getInstanceId());
            self.updateCurProposalNo(req.getProposalNo());
            logManager.getLock().writeLock().unlock();
        }
    }

    @Override
    public void handleConfirmRequest(final ConfirmReq req) {
        handleConfirmRequest(req, fakeCallback);
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
            if (Master.ElectState.allowBoost(RoleAccessor.getMaster().electState())) {
                LOG.debug("NO_SUPPORT, but i am master, try boost: {}", request.getInstanceId());

                List<Proposal> defaultValue = instance == null || CollectionUtils.isEmpty(instance.getGrantedValue())
                        ? Lists.newArrayList(Proposal.NOOP) : instance.getGrantedValue();
                CountDownLatch latch = new CountDownLatch(1);

                RoleAccessor.getProposer().tryBoost(new Holder<Long>() {
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

    private enum TaskEnum {
        APPLY, REPLAY, SNAP_LOAD, SNAP_TAKE;
    }

    private static class Task {
        private static final long HIGH_PRIORITY = -1;
        private long priority;
        private TaskEnum taskType;
        private TaskCallback callback;
        private List<Proposal> replayProposals;
        private Map<String, Snap> loadSnaps;

        Task(final long priority, final TaskEnum taskType, final TaskCallback callback) {
            this.priority = priority;
            this.taskType = taskType;
            this.callback = callback;
        }
    }

    private interface TaskCallback {
        default void onApply(final Map<Proposal, Object> result) {
        }

        default void onTakeSnap(final Map<String, Snap> snaps) {

        }

        default void onLoadSnap() {
        }
    }
}
