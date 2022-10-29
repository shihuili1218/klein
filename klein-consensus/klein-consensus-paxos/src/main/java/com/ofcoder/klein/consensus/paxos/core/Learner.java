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

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.MemberManager;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.rpc.vo.ConfirmReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.PrepareRes;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.InvokeParam;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.rpc.facade.RpcContext;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.rpc.facade.RpcProcessor;
import com.ofcoder.klein.rpc.facade.serialization.Hessian2Util;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author 释慧利
 */
public class Learner implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Learner.class);
    private RpcClient client;
    private final PaxosNode self;
    private LogManager logManager;
    private SM sm;
    private BlockingQueue<Long> applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(Long::longValue));
    private ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create("audit-predict", true));
    private CountDownLatch shutdownLatch;
    private ConsensusProp prop;
    private final Map<Long, CountDownLatch> boostingLatch = new ConcurrentHashMap<>();

    public Learner(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        this.prop = op;
        logManager = StorageEngine.getLogManager();
        this.client = RpcEngine.getClient();

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
                sm.makeImage();
            } finally {
                shutdownLatch.countDown();
            }
        });
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
        }

    }

    public void loadSM(SM sm) {
        this.sm = sm;
    }


    /**
     * The method blocks until instance changes to confirm
     *
     * @param instanceId id of the instance that you want to learn
     */
    public void boost(long instanceId) {
        LOG.info("start boost, instanceId: {}", instanceId);

        CountDownLatch latch = new CountDownLatch(1);
        if (boostingLatch.putIfAbsent(instanceId, latch) != null) {
            latch = boostingLatch.get(instanceId);
        }

        Instance instance = logManager.getInstance(instanceId);
        Object localValue = instance != null ? instance.getGrantedValue() : null;
        localValue = localValue == null ? Instance.Noop.DEFAULT : localValue;
        ProposeContext ctxt = new ProposeContext(instanceId, localValue, Lists.newArrayList(result -> {
            if (Result.SUCCESS.equals(result)) {

            } else {
                boost(instanceId);
            }
        }));
        RoleAccessor.getProposer().prepare(ctxt);

        try {
            latch.wait(2000);
        } catch (InterruptedException e) {
            boost(instanceId);
        }
        boostingLatch.remove(instanceId);
    }

    public void learn(long instanceId, Endpoint target) {
        LOG.info("start learn, instanceId: {}", instanceId);

        LearnReq req = LearnReq.Builder.aLearnReq().instanceId(instanceId).nodeId(self.getSelf().getId()).build();
        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(LearnReq.class.getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        client.sendRequestAsync(target, param, new AbstractInvokeCallback<LearnRes>() {
            @Override
            public void error(Throwable err) {
                LOG.warn(err.getMessage());
                // do nothing
            }

            @Override
            public void complete(LearnRes result) {
                LOG.info("node-{} learn result: {}", target.getId(), result);
                if (result.getInstance() == null) {
                    LOG.info("learn instance: {} from node-{}, but result.instance is null", instanceId, target.getId());
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


    private void apply(long instanceId) {
        if (instanceId <= logManager.maxAppliedInstanceId()) {
            // the instance has been applied.
            return;
        }
        long exceptConfirmId = logManager.maxAppliedInstanceId() + 1;
        if (instanceId > exceptConfirmId) {
            long pre = instanceId - 1;
            Instance preInstance = logManager.getInstance(pre);
            if (preInstance != null && preInstance.getState() == Instance.State.CONFIRMED) {
                apply(pre);
            } else {
                boost(pre);
                apply(pre);
            }
        }

        // update log to applied.
        Instance localInstance;
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

        if (localInstance.getGrantedValue() instanceof List) {
            // input state machine
            for (Object data : (List<Object>) localInstance.getGrantedValue()) {
                try {
                    sm.apply(data);
                } catch (Exception e) {
                    LOG.warn(String.format("apply instance[%s] to sm, %s", instanceId, e.getMessage()), e);
                }
            }
        } else if (localInstance.getGrantedValue() instanceof Instance.Noop) {
            //do nothing
        } else {
            LOG.error("apply instance[{}] to sm, UNKNOWN PARAMETER TYPE", instanceId);
        }

    }


    /**
     * Send confirm message.
     *
     * @param instanceId id of the instance
     * @param data      data in instance
     */
    public void confirm(long instanceId, Object data) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);
        ConfirmReq req = ConfirmReq.Builder.aConfirmReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .instanceId(instanceId)
                .data(data)
                .build();

        InvokeParam param = InvokeParam.Builder.anInvokeParam()
                .service(ConfirmReq.class.getSimpleName())
                .method(RpcProcessor.KLEIN)
                .data(ByteBuffer.wrap(Hessian2Util.serialize(req))).build();

        MemberManager.getAllMembers().forEach(it -> {
            client.sendRequestAsync(it, param, new AbstractInvokeCallback<PrepareRes>() {
                @Override
                public void error(Throwable err) {
                    LOG.warn(err.getMessage());
                    // do nothing
                }

                @Override
                public void complete(PrepareRes result) {
                    LOG.info("node-{} confirm result: {}", it.getId(), result);
                    // do nothing
                }
            }, 1000);
        });
    }

    /**
     * Processing confirm message with Learner.
     *
     * @param req message
     */
    public void handleConfirmRequest(ConfirmReq req) {

        try {
            logManager.getLock().writeLock().lock();

            Instance localInstance = logManager.getInstance(req.getInstanceId());
            if (localInstance == null) {
                // the prepare message is not received, the confirm message is received.
                // however, the instance has reached confirm, indicating that it has reached a consensus.
                localInstance = Instance.Builder.anInstance()
                        .instanceId(req.getInstanceId())
                        .applied(new AtomicBoolean(false))
                        .build();

                long diffId = req.getInstanceId() - self.getCurInstanceId();
                if (diffId > 0) {
                    self.addInstanceId(diffId);
                }
            }
            if (localInstance.getState() == Instance.State.CONFIRMED) {
                // the instance is confirmed.
                return;
            }
            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setProposalNo(req.getProposalNo());
            localInstance.setGrantedValue(req.getData());
            logManager.updateInstance(localInstance);

            // apply statemachine
            applyQueue.offer(req.getInstanceId());
        } finally {
            logManager.getLock().writeLock().unlock();
        }

        if (boostingLatch.containsKey(req.getInstanceId())) {
            boostingLatch.get(req.getInstanceId()).countDown();
        }
    }

    public void handleLearnRequest(LearnReq request, RpcContext context) {
        Instance instance = logManager.getInstance(request.getInstanceId());
        LearnRes res = LearnRes.Builder.aLearnRes().instance(instance).nodeId(self.getSelf().getId()).build();
        context.response(ByteBuffer.wrap(Hessian2Util.serialize(res)));
    }


}