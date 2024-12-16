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
package com.ofcoder.klein.consensus.facade.sm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.consensus.facade.Command;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.config.SnapshotStrategy;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * SM Applier.
 */
public class SMApplier {
    private static final Logger LOG = LoggerFactory.getLogger(SMApplier.class);
    private final String group;
    private final SM sm;
    private final BlockingQueue<Task> applyQueue;
    private boolean shutdown = false;
    private LogManager<Command> logManager;
    private Long lastAppliedId = 0L;
    private Long lastCheckpoint = 0L;
    private Long lastSnapTime = System.currentTimeMillis();
    private final List<SnapshotStrategy> snapshotStrategies;

    public SMApplier(final String group, final SM sm, final ConsensusProp op) {
        this.group = group;
        this.sm = sm;
        this.snapshotStrategies = op.getSnapshotStrategy();
        this.applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(value -> value.priority));
        this.logManager = ExtensionLoader.getExtensionLoader(LogManager.class).getJoin();
        ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create(this.group + "-apply", true));

        applyExecutor.execute(() -> {
            while (!shutdown) {
                try {
                    Task take = applyQueue.take();
                    switch (take.taskType) {
                        case APPLY:
                        case REPLAY:
                            _apply(take);
                            break;
                        case SNAP_LOAD:
                            _loadSnap(take);
                            break;
                        case SNAP_TAKE:
                            _takeSnap(take);
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

    private void _takeSnap(final Task task) {
        Snap lastSnap = logManager.getLastSnap(group);

        if (lastSnap == null || lastSnap.getCheckpoint() < lastAppliedId) {
            lastSnap = new Snap(lastAppliedId, sm.makeImage());
            this.lastCheckpoint = lastAppliedId;
            logManager.saveSnap(group, lastSnap);
            lastSnapTime = System.currentTimeMillis();
            LOG.info("take snapshot success, group: {}, cp: {}", group, lastSnap.getCheckpoint());
        }

        task.callback.onTakeSnap(lastSnap);
    }

    private void _loadSnap(final Task task) {
        Snap localSnap = logManager.getLastSnap(group);

        if (task.loadSnap.getCheckpoint() <= lastAppliedId) {
            LOG.warn("load snap skip, group: {}, local.lastAppliedId: {}, snap.checkpoint: {}", group, lastAppliedId, task.loadSnap.getCheckpoint());
        } else if (localSnap != null && localSnap.getCheckpoint() > task.loadSnap.getCheckpoint()) {
            LOG.warn("load snap skip, group: {}, local.checkpoint: {}, snap.checkpoint: {}", group, localSnap.getCheckpoint(), task.loadSnap.getCheckpoint());
        } else {
            sm.loadImage(task.loadSnap.getSnap());
            logManager.saveSnap(group, task.loadSnap);
            lastCheckpoint = task.loadSnap.getCheckpoint();
            lastAppliedId = task.loadSnap.getCheckpoint();
            // todo: callback.onApply removeIf
            this.applyQueue.removeIf(it -> it.priority != Task.HIGH_PRIORITY && it.priority < task.loadSnap.getCheckpoint());
            LOG.info("load snap success, group: {}, checkpoint: {}", group, task.loadSnap.getCheckpoint());
        }

        task.callback.onLoadSnap(lastCheckpoint);
    }

    private void _apply(final Task task) {

        final long lastApplyId = this.lastAppliedId;
        final long instanceId = task.priority;

        Map<Command, Object> applyResult = new HashMap<>();
        if (instanceId > lastApplyId) {
            LOG.debug("doing apply instance[{}]", instanceId);
            Instance<Command> instance = logManager.getInstance(instanceId);
            List<Command> proposals = instance.getGrantedValue().stream()
                    .filter(Command::ifNoop)
                    .filter(it -> group.equals(it.getGroup()))
                    .collect(Collectors.toList());

            proposals.forEach(it -> applyResult.put(it, sm.apply(it.getData())));
            this.lastAppliedId = instanceId;
        }

        // the instance has been applied.
        task.callback.onApply(applyResult);

        if (task.taskType == TaskEnum.APPLY) {
            checkAndSnapshot();
        }
    }

    private void checkAndSnapshot() {
        long now = System.currentTimeMillis();
        long applySize = lastAppliedId - lastCheckpoint;
        long lastSnapshotInterval = (now - lastSnapTime) / 1000;
        if (snapshotStrategies.stream().anyMatch(it -> lastSnapshotInterval >= it.getSecond() && applySize >= it.getReqCount())) {
            offer(Task.createTakeSnapTask(Task.FAKE_CALLBACK));
        }
        // else: do nothing
    }

    /**
     * offer task.
     *
     * @param t task
     */
    public void offer(final Task t) {
        boolean offer = applyQueue.offer(t);
        if (!offer) {
            LOG.error("failed to push the instance[{}] to the applyQueue, applyQueue.size: {}.", t.priority, applyQueue.size());
        }
        // do nothing, other threads will boost the instance
    }

    /**
     * close sm.
     */
    public void close() {
        CountDownLatch latch = new CountDownLatch(1);
        offer(Task.createTakeSnapTask(new TaskCallback() {
            @Override
            public void onTakeSnap(final Snap snap) {
                latch.countDown();
            }
        }));

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                LOG.error("generate snapshot timeout. lastApplyId: {}, lastCheckpoint: {}", lastAppliedId, lastCheckpoint);
            }
        } catch (InterruptedException ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
        }

        shutdown = true;
        sm.close();
    }

    /**
     * Get Last Applied Instance Id.
     *
     * @return Instance Id
     */
    public long getLastAppliedId() {
        return lastAppliedId;
    }

    public long getLastCheckpoint() {
        return lastCheckpoint;
    }

    public enum TaskEnum {
        APPLY, REPLAY, SNAP_LOAD, SNAP_TAKE
    }

    public static final class Task {
        public static final long HIGH_PRIORITY = -1;
        private static final SMApplier.TaskCallback FAKE_CALLBACK = new SMApplier.TaskCallback() {
        };
        private long priority;
        private TaskEnum taskType;
        private TaskCallback callback;
        private Snap loadSnap;

        private Task() {
        }

        /**
         * create task for TaskEnum.SNAP_TAKE.
         *
         * @param callback call back
         * @return Task Object
         */
        public static Task createTakeSnapTask(final TaskCallback callback) {
            Task task = new Task();
            task.priority = HIGH_PRIORITY;
            task.taskType = TaskEnum.SNAP_TAKE;
            task.callback = callback;
            return task;
        }

        /**
         * create task for TaskEnum.SNAP_LOAD.
         *
         * @param loadSnap snapshot
         * @param callback load snap callback
         * @return Task Object
         */
        public static Task createLoadSnapTask(final Snap loadSnap, final TaskCallback callback) {
            Task task = new Task();
            task.priority = HIGH_PRIORITY;
            task.taskType = TaskEnum.SNAP_LOAD;
            task.loadSnap = loadSnap;
            task.callback = callback;
            return task;
        }

        /**
         * create task for TaskEnum.APPLY.
         *
         * @param instanceId apply instance id
         * @param callback   apply callback
         * @return task object
         */
        public static Task createApplyTask(final long instanceId, final TaskCallback callback) {
            Task task = new Task();
            task.priority = instanceId;
            task.taskType = TaskEnum.APPLY;
            task.callback = callback;
            return task;
        }

        /**
         * create task for TaskEnum.REPLAY.
         *
         * @param instanceId apply instance id
         * @return task object
         */
        public static Task createReplayTask(final long instanceId) {
            Task task = new Task();
            task.priority = instanceId;
            task.taskType = TaskEnum.REPLAY;
            task.callback = Task.FAKE_CALLBACK;
            return task;
        }
    }

    public interface TaskCallback {
        /**
         * call after apply.
         *
         * @param result apply result
         */
        default void onApply(final Map<Command, Object> result) {

        }

        /**
         * call after take snapshot.
         *
         * @param snap snapshot
         */
        default void onTakeSnap(final Snap snap) {

        }

        /**
         * call after load snapshot.
         *
         * @param checkpoint snapshot's checkpoint
         */
        default void onLoadSnap(final long checkpoint) {

        }
    }
}
