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

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.consensus.facade.AbstractInvokeCallback;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.Proposal;
import com.ofcoder.klein.consensus.paxos.core.sm.MemberRegistry;
import com.ofcoder.klein.consensus.paxos.core.sm.PaxosMemberConfiguration;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.LearnRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncReq;
import com.ofcoder.klein.consensus.paxos.rpc.vo.SnapSyncRes;
import com.ofcoder.klein.consensus.paxos.rpc.vo.Sync;
import com.ofcoder.klein.rpc.facade.Endpoint;
import com.ofcoder.klein.rpc.facade.RpcClient;
import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataAligner {
    private static final Logger LOG = LoggerFactory.getLogger(DataAligner.class);
    private final PaxosNode self;
    private ConsensusProp prop;
    private final PaxosMemberConfiguration memberConfig;
    private LogManager<Proposal> logManager;
    private RpcClient client;
    private final BlockingQueue<Task> learnQueue;
    private boolean shutdown = false;
    private Serializer serializer;

    public DataAligner(final PaxosNode self) {
        this.self = self;
        this.learnQueue = new PriorityBlockingQueue<>(1024, Comparator.comparingLong(value -> value.priority));
        this.memberConfig = MemberRegistry.getInstance().getMemberConfiguration();
        this.serializer = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");
        ExecutorService alignerExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create("data-aligner", true));
        alignerExecutor.execute(() -> {
            while (!shutdown) {
                try {
                    Task take = learnQueue.take();
                    switch (take.taskType) {
                        case DIFF:
                            _learn(take.priority, take.target, take.callback);
                            break;
                        case SNAP:
                            _snapSync(take.target, take.callback);
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

    void init(final ConsensusProp op) {
        this.prop = op;
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        this.client = ExtensionLoader.getExtensionLoader(RpcClient.class).getJoin();
    }

    void close() {
        this.shutdown = true;
    }

    /**
     * Send the learn message to <code>target</code>.
     *
     * @param instanceId instance to learn
     * @param target     learn objective
     * @param callback   Callbacks of learning results
     */
    public void diff(final long instanceId, final Endpoint target, final LearnCallback callback) {
        learnQueue.offer(new Task(instanceId, TaskEnum.DIFF, target, callback));
    }

    /**
     * Send the snap message to <code>target</code>.
     *
     * @param target   learn objective
     * @param callback Callbacks of learning results
     */
    public void snap(final Endpoint target, final LearnCallback callback) {
        learnQueue.offer(new Task(Task.HIGH_PRIORITY, TaskEnum.SNAP, target, callback));
    }

    private void _learn(final long instanceId, final Endpoint target, final LearnCallback callback) {

        Instance<Proposal> instance = logManager.getInstance(instanceId);
        if (instance != null && instance.getState() == Instance.State.CONFIRMED) {
            callback.learned(true);
            return;
        }

        LOG.info("start learn instanceId[{}] from node-{}", instanceId, target.getId());
        LearnReq req = LearnReq.Builder.aLearnReq().instanceId(instanceId).nodeId(self.getSelf().getId()).build();
        client.sendRequestAsync(target, serializer.serialize(req), new AbstractInvokeCallback<LearnRes>() {
            @Override
            public void error(final Throwable err) {
                LOG.error("learned instance[{}] from node-{}, {}", instanceId, target.getId(), err.getMessage());
                callback.learned(false);
            }

            @Override
            public void complete(final LearnRes result) {
                if (result.isResult() == Sync.SNAP) {
                    LOG.info("learned instance[{}] from node-{}, sync.type: SNAP", instanceId, target.getId());
                    learnQueue.offer(new Task(Task.HIGH_PRIORITY, TaskEnum.SNAP, target, callback));

                } else if (result.isResult() == Sync.SINGLE) {
                    LOG.info("learned instance[{}] from node-{}, sync.type: SINGLE", instanceId, target.getId());
                    Instance<Proposal> update = Instance.Builder.<Proposal>anInstance()
                        .instanceId(instanceId)
                        .proposalNo(result.getInstance().getProposalNo())
                        .grantedValue(result.getInstance().getGrantedValue())
                        .state(result.getInstance().getState())
                        .build();

                    try {
                        logManager.getLock(instanceId).writeLock().lock();
                        logManager.updateInstance(update);
                        callback.learned(true);
                    } catch (Exception e) {
                        callback.learned(false);
                    } finally {
                        logManager.getLock(instanceId).writeLock().unlock();
                    }
                } else {
                    LOG.info("learned instance[{}] from node-{}, sync.type: NO_SUPPORT", instanceId, target.getId());
                    callback.learned(false);
                }
            }
        }, 1000);
    }

    private void _snapSync(final Endpoint target, final LearnCallback callback) {
        LOG.info("start snap sync from node-{}", target.getId());

        boolean result = false;
        long checkpoint = -1;
        try {
            SnapSyncReq req = SnapSyncReq.Builder.aSnapSyncReq()
                .nodeId(self.getSelf().getId())
                .proposalNo(self.getCurProposalNo())
                .memberConfigurationVersion(memberConfig.getVersion())
                .checkpoint(RuntimeAccessor.getLearner().getLastCheckpoint())
                .build();
            SnapSyncRes res = (SnapSyncRes) serializer.deserialize(client.sendRequestSync(target, serializer.serialize(req), 1000));

            RuntimeAccessor.getLearner().loadSnapSync(res.getImages());
            checkpoint = res.getImages().values().stream().max(Comparator.comparingLong(Snap::getCheckpoint)).orElse(new Snap(checkpoint, null)).getCheckpoint();

            long finalCheckpoint = checkpoint;
            Predicate<Task> successPredicate = it -> it.priority != Task.HIGH_PRIORITY && it.priority < finalCheckpoint;
            this.learnQueue.stream().filter(successPredicate).forEach(it -> it.callback.learned(true));
            this.learnQueue.removeIf(successPredicate);
            result = true;

        } catch (Throwable e) {
            LOG.error("pull snap from {} fail. {}", target, e.getMessage());
        } finally {
            callback.learned(result);
        }
    }

    interface LearnCallback {
        void learned(boolean result);
    }

    public enum TaskEnum {
        DIFF, SNAP
    }

    private static final class Task {

        public static final long HIGH_PRIORITY = -1;
        private static final LearnCallback FAKE_CALLBACK = result -> {
        };
        private long priority;
        private TaskEnum taskType;
        private Endpoint target;
        private LearnCallback callback;

        private Task(final long priority, final TaskEnum taskType, final Endpoint target, final LearnCallback callback) {
            this.priority = priority;
            this.taskType = taskType;
            this.target = target;
            this.callback = callback;
        }
    }

}
