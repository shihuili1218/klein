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

package com.ofcoder.klein.consensus.paxos.handler;

import com.google.common.collect.Lists;
import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;
import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.consensus.paxos.PaxosNode;
import com.ofcoder.klein.consensus.paxos.core.Learner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 快照触发器.
 */
public abstract class SnapshotHandler implements Learner {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotHandler.class);

    protected PaxosNode self;

    /**
     * write time.
     */
    private final AtomicLong lastWriteMs = new AtomicLong(TrueTime.currentTimeMillis());

    private List<Map.Entry<Integer, Long>> rdbConfigList = Lists.newArrayListWithExpectedSize(10);
    private RepeatedTimer timer;

    private TimeUnit timeUnit;

    public SnapshotHandler(final PaxosNode self, final TimeUnit timeUnit) {
        this.self = self;
        this.timeUnit = timeUnit;
    }

    @Override
    public void init(final ConsensusProp op) {
        timer = new RepeatedTimer(SnapshotHandler.class.getSimpleName(), timeUnit.toMillis(1)) {
            @Override
            protected void onTrigger() {
                snapshot();
            }
        };

        // load rdb.config
        for (String item : StringUtils.split(op.getSnapshot(), ";")) {
            if (StringUtils.isEmpty(item)) {
                continue;
            }
            String[] minuteAndTime = item.split("\\ ");
            rdbConfigList.add(new AbstractMap.SimpleEntry<>(Integer.parseInt(minuteAndTime[0]), Long.parseLong(minuteAndTime[1])));
        }

        timer.start();
    }

    @Override
    public void shutdown() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void snapshot() {
        long sinceLastTime = (TrueTime.currentTimeMillis() - lastWriteMs.get()) / timeUnit.toMillis(1);
        for (Map.Entry<Integer, Long> minuteAndTime : rdbConfigList) {
            long writeCount = getWriteCount();
            if (minuteAndTime.getKey() <= sinceLastTime
                    && minuteAndTime.getValue() <= writeCount) {
                LOG.debug("时间间隔：{}， 写入次数：{}", sinceLastTime, writeCount);
                this.generateSnap();
                break;
            }
        }
    }

    private long getWriteCount() {
        long lastAppliedInstanceId = this.getLastAppliedInstanceId();
        return lastAppliedInstanceId - self.getLastCheckpoint();
    }

    public List<Map.Entry<Integer, Long>> getRdbConfigList() {
        return rdbConfigList;
    }

    public RepeatedTimer getTimer() {
        return timer;
    }
}
