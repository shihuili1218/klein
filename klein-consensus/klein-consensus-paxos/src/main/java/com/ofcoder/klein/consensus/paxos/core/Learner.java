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

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.Lifecycle;
import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.consensus.facade.SM;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author 释慧利
 */
public class Learner implements Lifecycle<ConsensusProp> {
    private static final Logger LOG = LoggerFactory.getLogger(Learner.class);
    private final PaxosNode self;
    private LogManager logManager;
    private SM sm;
    private BlockingQueue<ApplyEntry> applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(ApplyEntry::getInstanceId));
    private ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create("audit-predict", true));
    private CountDownLatch shutdownLatch;


    public Learner(PaxosNode self) {
        this.self = self;
    }

    @Override
    public void init(ConsensusProp op) {
        logManager = StorageEngine.getLogManager();

        applyExecutor.execute(() -> {
            while (shutdownLatch == null) {
                try {
                    ApplyEntry take = applyQueue.take();
                    apply(take.instanceId, take.datas);
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

    public void learn(long instanceId) {
        LOG.info("start learn, instanceId: {}", instanceId);
        ProposeContext ctxt = new ProposeContext(instanceId, Lists.newArrayList(Noop.DEFAULT), Lists.newArrayList());
        RoleAccessor.getProposer().forcePrepare(ctxt, new PhaseCallback.PreparePhaseCallback() {
            @Override
            public void granted(ProposeContext context) {
                RoleAccessor.getProposer().ac
            }

            @Override
            public void confirmed(ProposeContext context) {
                confirm(context.getInstanceId(), context.getDatas());
            }

            @Override
            public void refused(ProposeContext context) {
                learn(context.getInstanceId());
            }
        });
    }


    private void apply(long instanceId, List<Object> datas) {
        if (instanceId <= logManager.maxConfirmInstanceId()) {
            // the Instance has been confirmed.
            return;
        }
        long exceptConfirmId = logManager.maxConfirmInstanceId() + 1;
        if (instanceId > exceptConfirmId) {
            learn(instanceId - 1);
        }

        // update log to applied.
        try {
            logManager.getLock().writeLock().lock();

            Instance localInstance = logManager.getInstance(instanceId);
            if (!localInstance.getApplied().compareAndSet(false, true)) {
                return ;
            }
            logManager.updateInstance(localInstance);
        } finally {
            logManager.getLock().writeLock().unlock();
        }

        for (Object data : datas) {
            try {
                sm.apply(data);
            } catch (Exception e) {
                LOG.warn(String.format("apply instance[%s] to sm, %s", instanceId, e.getMessage()), e);
            }
        }
    }


    public void confirm(long instanceId, List<Object> datas) {
        LOG.info("start confirm phase, instanceId: {}", instanceId);
        try {
            logManager.getLock().writeLock().lock();

            Instance localInstance = logManager.getInstance(instanceId);
            if (localInstance == null) {
                // the prepare message is not received, but the confirm message is received
                learn(instanceId);
                return;
            }
            localInstance.setState(Instance.State.CONFIRMED);
            localInstance.setGrantedValue(datas);
            logManager.updateInstance(localInstance);

            // apply statemachine
            applyQueue.offer(new ApplyEntry(instanceId, datas));
        } finally {
            logManager.getLock().writeLock().unlock();
        }
    }

    private static class ApplyEntry {
        private long instanceId;
        private List<Object> datas;

        public ApplyEntry(long instanceId, List<Object> datas) {
            this.instanceId = instanceId;
            this.datas = datas;
        }

        public long getInstanceId() {
            return instanceId;
        }
    }


}
