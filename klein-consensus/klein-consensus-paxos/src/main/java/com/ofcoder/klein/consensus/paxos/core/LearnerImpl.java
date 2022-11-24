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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
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
 * @author 释慧利
 */
public class LearnerImpl implements Learner {
    private static final Logger LOG = LoggerFactory.getLogger(LearnerImpl.class);
    private RpcClient client;
    private final PaxosNode self;
    private LogManager<Proposal> logManager;
    private final ConcurrentMap<String, SM> sms = new ConcurrentHashMap<>();
    private final BlockingQueue<Long> applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(Long::longValue));
    private final ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create("apply-instance", true));
    private CountDownLatch shutdownLatch;
    private final Map<Long, List<Learner.ApplyCallback>> applyCallback = new ConcurrentHashMap<>();
    private final ReentrantLock snapLock = new ReentrantLock();
    private final Object waitMaster = new Object();

    public LearnerImpl(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();

        applyExecutor.execute(() -> {
            while (shutdownLatch == null) {
                try {
                    long take = applyQueue.take();
                    apply(take);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        RoleAccessor.getMaster().addHealthyListener(new Master.HealthyListener() {
            @Override
            public void change(boolean healthy) {
                waitMaster.notifyAll();
            }
        });
    }

    @Override
    public void shutdown() {
        shutdownLatch = new CountDownLatch(1);
        ThreadExecutor.submit(() -> {
            try {
                generateSnap();
            } finally {
                shutdownLatch.countDown();
            }
        });
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private void generateSnap() {
        try {
            if ((snapLock.isLocked() && !snapLock.isHeldByCurrentThread())
                    || (!snapLock.isLocked() && !snapLock.tryLock())) {
                return;
            }

            for (Map.Entry<String, SM> entry : sms.entrySet()) {
                Snap snapshot = entry.getValue().snapshot();
                self.updateLastCheckpoint(snapshot.getCheckpoint());
                logManager.saveSnap(entry.getKey(), snapshot);
            }
        } finally {
            snapLock.unlock();
        }
    }

    private void loadSnap(String group, Snap lastSnap) {
        if (lastSnap == null) {
            return;
        }
        LOG.info("load snap, group: {}, checkpoint: {}", group, lastSnap.getCheckpoint());
        try {
            if ((snapLock.isLocked() && !snapLock.isHeldByCurrentThread())
                    || (!snapLock.isLocked() && !snapLock.tryLock())) {
                return;
            }
            SM sm = sms.get(group);

            sm.loadSnap(lastSnap);
            self.updateLastCheckpoint(lastSnap.getCheckpoint());
            LOG.info("load snap success, group: {}, checkpoint: {}", group, lastSnap.getCheckpoint());
        } finally {
            snapLock.unlock();
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, sm) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        loadSnap(group, lastSnap);
    }

    public void apply(long instanceId) {
        final long maxAppliedInstanceId = self.getCurAppliedInstanceId();
        final long lastCheckpoint = self.getLastCheckpoint();
        LOG.info("start apply, instanceId: {}, curAppliedInstanceId: {}, lastCheckpoint: {}", instanceId, maxAppliedInstanceId, lastCheckpoint);
        if (instanceId <= maxAppliedInstanceId || instanceId <= lastCheckpoint) {
            // the instance has been applied.
            return;
        }
        long exceptConfirmId = maxAppliedInstanceId + 1;
        if (instanceId > exceptConfirmId) {
            long pre = instanceId - 1;
            Instance<Proposal> preInstance = logManager.getInstance(pre);
            if (preInstance == null || preInstance.getState() != Instance.State.CONFIRMED) {
                Endpoint master = self.getMemberConfiguration().getMaster();
                if (master == null) {
                    synchronized (waitMaster) {
                        try {
                            waitMaster.wait(500);
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                    }
                }
                master = self.getMemberConfiguration().getMaster();
                if (master == null || !learnSync(pre, master)) {
                    applyQueue.add(pre);
                    applyQueue.add(instanceId);
                    return;
                }
            }
            applyQueue.add(pre);
            return;
        }

        // update log to applied.
        Instance<Proposal> localInstance;
        try {
            logManager.getLock().writeLock().lock();

            localInstance = logManager.getInstance(instanceId);
            if (!localInstance.getApplied().compareAndSet(false, true)) {
                // the instance has been applied.
                return;
            }
            logManager.updateInstance(localInstance);
            self.setCurAppliedInstanceId(instanceId);
        } finally {
            logManager.getLock().writeLock().unlock();
        }

        try {
            List<ApplyCallback> callback = applyCallback.getOrDefault(instanceId, Lists.newArrayList(new DefaultApplyCallback()));
            for (Proposal data : localInstance.getGrantedValue()) {
                Object result = this._apply(localInstance.getInstanceId(), data);
                callback.forEach(it -> it.apply(data, result));
            }
        } finally {
            applyCallback.remove(instanceId);
        }
    }

    private Object _apply(long instance, Proposal data) {
        LOG.info("doing apply instance[{}]", instance);
        if (data.getData() instanceof Proposal.Noop) {
            //do nothing
            return null;
        }
        if (snapLock.isLocked()) {
            return _apply(instance, data);
        }

        if (instance <= self.getLastCheckpoint()) {
            //do nothing
            return null;
        }

        if (sms.containsKey(data.getGroup())) {
            SM sm = sms.get(data.getGroup());
            try {
                return sm.apply(instance, data.getData());
            } catch (Exception e) {
                LOG.warn(String.format("apply instance[%s] to sm, %s", instance, e.getMessage()), e);
                return null;
            }
        } else {
            LOG.error("the group[{}] is not loaded with sm, and the instance[{}] is not applied", data.getGroup(), instance);
            return null;
        }

    }

    @Override
    public void learn(long instanceId, Endpoint target, LearnCallback callback) {
        LOG.info("start learn instanceId[{}] from node-{}", instanceId, target.getId());

        Instance<Proposal> instance = logManager.getInstance(instanceId);
        if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
            callback.learned(true);
            return;
        }

        LearnReq req = LearnReq.Builder.aLearnReq().instanceId(instanceId).nodeId(self.getSelf().getId()).build();

        client.sendRequestAsync(target, req, new AbstractInvokeCallback<LearnRes>() {
            @Override
            public void error(Throwable err) {
                LOG.error("learn instance[{}] from node-{}, {}", instanceId, target.getId(), err.getMessage());
                callback.learned(false);
            }

            @Override
            public void complete(LearnRes result) {
                if (result.isResult() == Sync.SNAP) {
                    LOG.info("learn instance[{}] from node-{}, sync.type: SNAP", instanceId, target.getId());
                    snapSync(target, callback);
                } else if (result.isResult() == Sync.SINGLE) {
                    LOG.info("learn instance[{}] from node-{}, sync.type: SINGLE", instanceId, target.getId());
                    Instance<Proposal> update = Instance.Builder.<Proposal>anInstance()
                            .instanceId(instanceId)
                            .proposalNo(result.getInstance().getProposalNo())
                            .grantedValue(result.getInstance().getGrantedValue())
                            .state(result.getInstance().getState())
                            .applied(new AtomicBoolean(false))
                            .build();
                    logManager.updateInstance(update);
                    // apply statemachine
                    if (!applyQueue.offer(req.getInstanceId())) {
                        LOG.error("failed to push the instance[{}] to the applyQueue, applyQueue.size = {}.", req.getInstanceId(), applyQueue.size());
                        // do nothing, other threads will boost the instance
                    }
                    callback.learned(true);
                } else {
                    LOG.info("learn instance[{}] from node-{}, sync.type: NO_SUPPORT", instanceId, target.getId());
                    callback.learned(false);
                }
            }
        }, 1000);
    }

    @Override
    public void keepSameData(final Endpoint target, final long checkpoint, final long maxAppliedInstanceId) {
        long curAppliedInstanceId = self.getCurAppliedInstanceId();
        if (checkpoint > curAppliedInstanceId) {
            ThreadExecutor.submit(() -> snapSync(target, new DefaultLearnCallback()));
        } else {
            long diff = maxAppliedInstanceId - curAppliedInstanceId;
            if (diff > 0) {
                ThreadExecutor.submit(() -> {
                    for (int i = 1; i <= diff; i++) {
                        RoleAccessor.getLearner().learn(curAppliedInstanceId + i, target);
                    }
                });
            }
        }
    }

    private void snapSync(Endpoint target, LearnCallback callback) {
        LOG.info("start snap sync from node-{}", target.getId());
        try {
            if (!snapLock.tryLock()) {
                return;
            }

            CompletableFuture<SnapSyncRes> future = new CompletableFuture<>();
            SnapSyncReq req = SnapSyncReq.Builder.aSnapSyncReq()
                    .nodeId(self.getSelf().getId())
                    .proposalNo(self.getCurProposalNo())
                    .memberConfigurationVersion(self.getMemberConfiguration().getVersion())
                    .checkpoint(self.getLastCheckpoint())
                    .build();
            client.sendRequestAsync(target, req, new AbstractInvokeCallback<SnapSyncRes>() {
                @Override
                public void error(Throwable err) {
                    LOG.error("snap sync from node-{}, {}", target.getId(), err.getMessage());
                    future.completeExceptionally(err);
                }

                @Override
                public void complete(SnapSyncRes result) {
                    future.complete(result);
                }
            }, 1000);

            SnapSyncRes res = future.get(1010, TimeUnit.MILLISECONDS);
            for (Map.Entry<String, Snap> entry : res.getImages().entrySet()) {
                loadSnap(entry.getKey(), entry.getValue());
            }
            callback.learned(true);
        } catch (Throwable e) {
            LOG.error(e.getMessage());
            callback.learned(false);
        } finally {
            snapLock.unlock();
        }
    }

    @Override
    public void confirm(long instanceId, final ApplyCallback callback) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);

        applyCallback.putIfAbsent(instanceId, new ArrayList<>());
        applyCallback.get(instanceId).add(callback);

        // A proposalNo here does not have to use the proposalNo of the accept phase,
        // because the proposal is already in the confirm phase and it will not change.
        // Instead, using self.proposalNo allows you to more quickly advance a proposalNo for another member
        long curProposalNo = self.getCurProposalNo();

        ConfirmReq req = ConfirmReq.Builder.aConfirmReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(curProposalNo)
                .instanceId(instanceId)
                .build();

        // for self
        handleConfirmRequest(req);

        // for other members
        self.getMemberConfiguration().getMembersWithoutSelf().forEach(it -> {
            client.sendRequestAsync(it, req, new AbstractInvokeCallback<Serializable>() {
                @Override
                public void error(Throwable err) {
                    LOG.error("send confirm msg to node-{}, instance[{}], {}", it.getId(), instanceId, err.getMessage());
                    // do nothing
                }

                @Override
                public void complete(Serializable result) {
                    // do nothing
                }
            }, 1000);
        });
    }

    @Override
    public void handleConfirmRequest(ConfirmReq req) {
        LOG.info("processing the confirm message from node-{}, instance: {}", req.getNodeId(), req.getInstanceId());

        if (req.getInstanceId() <= self.getLastCheckpoint()) {
            // the instance is compressed.
            LOG.info("the instance[{}] is compressed, checkpoint[{}]", req.getInstanceId(), self.getLastCheckpoint());
            return;
        }

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                LOG.info("confirm message is received, but accept message is not received, instance: {}", req.getInstanceId());
                learn(req.getInstanceId(), self.getMemberConfiguration().getEndpointById(req.getNodeId()));
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
            if (!applyQueue.offer(req.getInstanceId())) {
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
    public LearnRes handleLearnRequest(LearnReq request) {
        LOG.info("received a learn message from node[{}] about instance[{}]", request.getNodeId(), request.getInstanceId());
        LearnRes.Builder res = LearnRes.Builder.aLearnRes().nodeId(self.getSelf().getId());

        if (request.getInstanceId() <= self.getLastCheckpoint()) {
            return res.result(Sync.SNAP).build();
        }

        Instance<Proposal> instance = logManager.getInstance(request.getInstanceId());
        if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
            return res.result(Sync.SINGLE).instance(instance).build();
        } else {
            return res.result(Sync.NO_SUPPORT).build();
        }
    }

    @Override
    public SnapSyncRes handleSnapSyncRequest(SnapSyncReq req) {
        LOG.info("processing the pull snap message from node-{}", req.getNodeId());
        SnapSyncRes res = SnapSyncRes.Builder.aSnapSyncRes()
                .images(new HashMap<>())
                .checkpoint(self.getLastCheckpoint())
                .build();
        for (String group : sms.keySet()) {
            Snap lastSnap = logManager.getLastSnap(group);
            if (lastSnap.getCheckpoint() > req.getCheckpoint()) {
                res.getImages().put(group, lastSnap);
            }
        }
        return res;
    }
}
