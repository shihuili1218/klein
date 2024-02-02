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

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.KleinThreadFactory;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * SM Applier.
 */
public class SMApplier<P extends Serializable> {
    private static final Logger LOG = LoggerFactory.getLogger(SMApplier.class);
    private final String group;
    private final SM sm;
    private final BlockingQueue<Task<P>> applyQueue;
    private boolean shutdown = false;

    public SMApplier(final String group, final SM sm) {
        this.group = group;
        this.sm = sm;
        this.applyQueue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(value -> value.priority));
        ExecutorService applyExecutor = Executors.newFixedThreadPool(1, KleinThreadFactory.create(this.group + "-apply", true));

        applyExecutor.execute(() -> {
            while (!shutdown) {
                try {
                    Task<P> take = applyQueue.take();
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

    private void _takeSnap(final Task<P> task) {
        Snap snapshot = sm.snapshot();
        LOG.info("take snapshot success, group: {}, cp: {}", group, snapshot.getCheckpoint());
        task.callback.onTakeSnap(snapshot);
    }

    private void _loadSnap(final Task<P> task) {
        long lastCheckpoint = sm.lastCheckpoint();
        if (task.loadSnap.getCheckpoint() > sm.lastCheckpoint()) {
            sm.loadSnap(task.loadSnap);
            lastCheckpoint = task.loadSnap.getCheckpoint();
            this.applyQueue.removeIf(it -> it.priority != Task.HIGH_PRIORITY && it.priority < task.loadSnap.getCheckpoint());
        }

        task.callback.onLoadSnap(lastCheckpoint);
    }

    private void _apply(final Task<P> task) {

        final long lastApplyId = sm.lastAppliedId();
        final long instanceId = task.priority;

        Map<P, Object> applyResult = new HashMap<>();
        if (instanceId > lastApplyId) {
            LOG.debug("doing apply instance[{}]", instanceId);
            applyResult = task.proposals.stream().collect(Collectors.toMap(p -> p, p -> sm.apply(instanceId, p)));
        }

        // the instance has been applied.
        task.callback.onApply(applyResult);
    }

    /**
     * offer task.
     *
     * @param t task
     */
    public void offer(final Task<P> t) {
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
        shutdown = true;
        sm.close();
    }

    /**
     * Get Last Applied Instance Id.
     *
     * @return Instance Id
     */
    public long getLastAppliedId() {
        return sm.lastAppliedId();
    }

    public enum TaskEnum {
        APPLY, REPLAY, SNAP_LOAD, SNAP_TAKE
    }

    public static final class Task<P extends Serializable> {
        public static final long HIGH_PRIORITY = -1;
        private long priority;
        private TaskEnum taskType;
        private TaskCallback<P> callback;
        private List<P> proposals;
        private Snap loadSnap;

        private Task() {
        }

        /**
         * create task for TaskEnum.SNAP_TAKE.
         *
         * @param callback call back
         * @param <P>      Consensus Proposal
         * @return Task Object
         */
        public static <P extends Serializable> Task<P> createTakeSnapTask(final TaskCallback<P> callback) {
            Task<P> task = new Task<>();
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
         * @param <P>      Consensus Proposal
         * @return Task Object
         */
        public static <P extends Serializable> Task<P> createLoadSnapTask(final Snap loadSnap, final TaskCallback<P> callback) {
            Task<P> task = new Task<>();
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
         * @param proposals  apply proposal
         * @param callback   apply callbacl
         * @param <P>        consensus proposal
         * @return task object
         */
        public static <P extends Serializable> Task<P> createApplyTask(final long instanceId, final List<P> proposals, final TaskCallback<P> callback) {
            Task<P> task = new Task<>();
            task.priority = instanceId;
            task.taskType = TaskEnum.APPLY;
            task.proposals = proposals;
            task.callback = callback;
            return task;
        }

        /**
         * create task for TaskEnum.REPLAY.
         *
         * @param instanceId apply instance id
         * @param proposals  apply proposal
         * @param callback   apply callbacl
         * @param <P>        consensus proposal
         * @return task object
         */
        public static <P extends Serializable> Task<P> createReplayTask(final long instanceId, final List<P> proposals, final TaskCallback<P> callback) {
            Task<P> task = new Task<>();
            task.priority = instanceId;
            task.taskType = TaskEnum.REPLAY;
            task.proposals = proposals;
            task.callback = callback;
            return task;
        }
    }

    public interface TaskCallback<P> {
        /**
         * call after apply.
         *
         * @param result apply result
         */
        default void onApply(final Map<P, Object> result) {

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
