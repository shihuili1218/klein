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
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.sm.SM;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
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
    private final Map<Long, List<ProposalWithDone>> applyCallback = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CompletableFuture<Result.State>> boostFuture = new ConcurrentHashMap<>();

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
        for (Map.Entry<String, SM> entry : sms.entrySet()) {
            Snap snapshot = entry.getValue().snapshot();
            logManager.saveSnap(entry.getKey(), snapshot);
        }
    }

    @Override
    public void keepFresh() {
        List<Instance<Proposal>> noConfirm = logManager.getInstanceNoConfirm();
        for (Instance<Proposal> instance : noConfirm) {
            boost(instance.getInstanceId());
        }
    }

    @Override
    public void loadSM(final String group, final SM sm) {
        if (sms.putIfAbsent(group, sm) != null) {
            LOG.error("the group[{}] has been loaded with sm.", group);
            return;
        }
        Snap lastSnap = logManager.getLastSnap(group);
        if (lastSnap != null) {
            sm.loadSnap(lastSnap);
        }
    }

    private void apply(long instanceId) {
        LOG.info("start apply, instanceId: {}", instanceId);

        final long maxAppliedInstanceId = logManager.maxAppliedInstanceId();
        if (instanceId <= maxAppliedInstanceId) {
            // the instance has been applied.
            return;
        }
        long exceptConfirmId = maxAppliedInstanceId + 1;
        if (instanceId > exceptConfirmId) {
            long pre = instanceId - 1;
            Instance<Proposal> preInstance = logManager.getInstance(pre);
            if (preInstance != null && preInstance.getState() == Instance.State.CONFIRMED) {
                apply(pre);
            } else {
                boost(pre);
                apply(pre);
            }
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
        } finally {
            logManager.getLock().writeLock().unlock();
        }

        if (applyCallback.containsKey(instanceId)) {
            // is self
            List<ProposalWithDone> proposalWithDones = applyCallback.remove(instanceId);
            for (ProposalWithDone proposalWithDone : proposalWithDones) {
                Object result = this._apply(instanceId, proposalWithDone.getProposal());
                try {
                    proposalWithDone.getDone().applyDone(result);
                } catch (Exception e) {
                    LOG.warn(String.format("apply instance[%s] to sm, call apply done occur exception. %s", instanceId, e.getMessage()), e);
                }
            }

        } else {
            // input state machine
            for (Proposal data : localInstance.getGrantedValue()) {
                this._apply(localInstance.getInstanceId(), data);
            }
        }
    }

    private Object _apply(long instance, Proposal data) {
        LOG.info("doing apply instance[{}]", instance);
        if (data.getData() instanceof Proposal.Noop) {
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


    /**
     * The method blocks until instance changes to confirmed
     *
     * @param instanceId id of the instance that you want to learn
     */
    private void boost(long instanceId) {
        LOG.info("boosting instanceId: {}", instanceId);
        Instance<Proposal> instance = logManager.getInstance(instanceId);
        if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
            return;
        }

        final CompletableFuture<Result.State> future = new CompletableFuture<>();
        if (boostFuture.putIfAbsent(instanceId, future) != null) {
            blockBoost(instanceId);
            return;
        }

        RoleAccessor.getProposer().boost(instanceId, new ProposeDone() {
            @Override
            public void negotiationDone(Result.State result) {
                if (result != Result.State.SUCCESS) {
                    future.complete(result);
                }
            }

            @Override
            public void confirmDone() {
                future.complete(Result.State.SUCCESS);
            }
        });

        blockBoost(instanceId);
    }

    private void blockBoost(long instanceId) {
        try {

            CompletableFuture<Result.State> future = boostFuture.get(instanceId);
            if (future != null) {
                Result.State state = future.get(2000, TimeUnit.MILLISECONDS);
                if (state != Result.State.SUCCESS) {
                    boost(instanceId);
                }
            }
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("{}, boost instance[{}] failure, {}", e.getClass().getName(), instanceId, e.getMessage());
            boost(instanceId);
        } finally {
            boostFuture.remove(instanceId);
        }
    }

    @Override
    public void learn(long instanceId, Endpoint target) {
        LOG.info("start learn instanceId[{}] from node-{}", instanceId, target.getId());

        LearnReq req = LearnReq.Builder.aLearnReq().instanceId(instanceId).nodeId(self.getSelf().getId()).build();

        client.sendRequestAsync(target, req, new AbstractInvokeCallback<LearnRes>() {
            @Override
            public void error(Throwable err) {
                LOG.error("learn instance[{}] from node-{}, {}", instanceId, target.getId(), err.getMessage());
                // do nothing
            }

            @Override
            public void complete(LearnRes result) {
                if (result.getInstance() == null) {
                    LOG.error("learn instance[{}] from node-{}, but result.instance is null", instanceId, target.getId());
                    return;
                }
                handleConfirmRequest(ConfirmReq.Builder.aConfirmReq()
                        .nodeId(result.getNodeId())
                        .proposalNo(result.getInstance().getProposalNo())
                        .instanceId(result.getInstance().getInstanceId())
                        .data(result.getInstance().getGrantedValue())
                        .build());
            }
        }, 1000);
    }

    @Override
    public void confirm(long instanceId, final List<ProposalWithDone> dataWithDone) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);

        applyCallback.putIfAbsent(instanceId, new ArrayList<>());
        applyCallback.get(instanceId).addAll(dataWithDone);

        // A proposalNo here does not have to use the proposalNo of the accept phase,
        // because the proposal is already in the confirm phase and it will not change.
        // Instead, using self.proposalNo allows you to more quickly advance a proposalNo for another member
        long curProposalNo = self.getCurProposalNo();

        ConfirmReq req = ConfirmReq.Builder.aConfirmReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(curProposalNo)
                .instanceId(instanceId)
                .data(dataWithDone.stream().map(ProposalWithDone::getProposal).collect(Collectors.toList()))
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

        self.setCurInstanceId(req.getInstanceId());
        self.setCurProposalNo(req.getProposalNo());

        try {
            logManager.getLock().writeLock().lock();

            Instance<Proposal> localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                // the accept message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                localInstance = Instance.Builder.<Proposal>anInstance()
                        .instanceId(req.getInstanceId())
                        .applied(new AtomicBoolean(false))
                        .build();
            }

            if (localInstance.getState() == Instance.State.CONFIRMED) {
                // the instance is confirmed.
                LOG.info("the instance[{}] is confirmed", localInstance.getInstanceId());
                return;
            }
            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setProposalNo(req.getProposalNo());
            localInstance.setGrantedValue(req.getData());
            logManager.updateInstance(localInstance);

            // apply statemachine
            if (!applyQueue.offer(req.getInstanceId())) {
                LOG.error("failed to push the instance[{}] to the applyQueue, applyQueue.size = {}.", req.getInstanceId(), applyQueue.size());
                // do nothing, other threads will boost the instance
            }

            if (applyCallback.containsKey(req.getInstanceId())) {
                List<ProposalWithDone> proposalWithDones = applyCallback.get(req.getInstanceId());
                for (ProposalWithDone proposalWithDone : proposalWithDones) {
                    try {
                        proposalWithDone.getDone().confirmDone();
                    } catch (Exception e) {
                        LOG.warn(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } finally {
            logManager.getLock().writeLock().unlock();
        }
    }

    @Override
    public LearnRes handleLearnRequest(LearnReq request) {
        LOG.info("received a learn message from node[{}] about instance[{}]", request.getNodeId(), request.getInstanceId());
        Instance<Proposal> instance = logManager.getInstance(request.getInstanceId());
        LearnRes res = LearnRes.Builder.aLearnRes().instance(instance).nodeId(self.getSelf().getId()).build();
        return res;
    }
}
