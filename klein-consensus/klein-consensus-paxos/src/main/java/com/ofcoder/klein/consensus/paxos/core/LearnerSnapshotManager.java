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
import com.ofcoder.klein.common.util.timer.DefaultTimer;
import com.ofcoder.klein.common.util.timer.Timeout;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.facade.exception.SnapshotException;
import com.ofcoder.klein.consensus.facade.sm.SMApplier;
import com.ofcoder.klein.storage.facade.Snap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LearnerSnapshotManager {
    private static final Logger LOG = LoggerFactory.getLogger(LearnerImpl.class);

    private static LearnerSnapshotManager instance;
    private final List<SnapshotStrategy> snapshotStrategies = Lists.newArrayList();
    private final Learner learner;
    private final DefaultTimer timer;
    private long lastReqTime = System.currentTimeMillis();
    private AtomicLong reqCount = new AtomicLong(0);

    private LearnerSnapshotManager(final Learner learner) {
        this.learner = learner;
        this.timer = new DefaultTimer(1, "LearnerSnapshotManager");
    }

    /**
     * initAndStart.
     *
     * @param op porp
     * @param learner learner
     */
    public static void initAndStart(final ConsensusProp op, final Learner learner) {
        instance = new LearnerSnapshotManager(learner);
        instance.snapshotStrategies.addAll(parseSnap(op.getSnapshotStrategy()));

        instance.start();
    }

    /**
     * start.
     */
    public void start() {
        timer.newTimeout(this::checkAndSnapshot, 0, TimeUnit.SECONDS);
    }

    private void checkAndSnapshot(final Timeout timeout) {
        long now = System.currentTimeMillis();
        long lastSnapshotInterval = (now - lastReqTime) / 1000;
        for (SnapshotStrategy snapshotStrategy : snapshotStrategies) {
            if (lastSnapshotInterval >= snapshotStrategy.getSecond() && reqCount.get() >= snapshotStrategy.getReqCount()) {
                lastReqTime = now;
                reqCount.set(0);
                generateSnap();
                return;
            }
        }
    }

    /**
     * addReqCount.
     */
    public void addReqCount() {
        reqCount.addAndGet(1);
    }

    /**
     * shutdown.
     */
    public void shutdown() {
        generateSnap();
        timer.stop();
    }

    private static List<SnapshotStrategy> parseSnap(final String prop) {
        String[] props = prop.split("\\ ");
        if (props.length % 2 != 0) {
            throw new SnapshotException("klein.snapshot config must appear in pairs.");
        }

        List<SnapshotStrategy> snapshotStrategies = Lists.newArrayList();
        for (int i = 0; i < props.length;) {
            SnapshotStrategy snapshotStrategy = new SnapshotStrategy(Integer.parseInt(props[i++]), Integer.parseInt(props[i++]));
            snapshotStrategies.add(snapshotStrategy);
        }
        return snapshotStrategies;
    }

    /**
     * generate and save snapshot.
     *
     * @return snaps
     */
    public Map<String, Snap> generateSnap() {
        Map<String, SMApplier> sms = new HashMap<>(learner.getSms());
        ConcurrentMap<String, Snap> result = new ConcurrentHashMap<>();
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
        });

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                LOG.error("generate snapshot timeout. succ: {}, all: {}", result.keySet(), sms.keySet());
            }
            return result;
        } catch (InterruptedException ex) {
            LOG.error(String.format("generate snapshot occur exception. %s", ex.getMessage()), ex);
            return result;
        }
    }

    public static LearnerSnapshotManager getInstance() {
        return instance;
    }

    public long getReqCount() {
        return reqCount.get();
    }

    public static class SnapshotStrategy {
        private int second;
        private int reqCount;

        public SnapshotStrategy(final int second, final int reqCount) {
            this.second = second;
            this.reqCount = reqCount;
        }

        public int getSecond() {
            return second;
        }

        public int getReqCount() {
            return reqCount;
        }
    }
}
