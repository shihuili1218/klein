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
package com.ofcoder.klein.common.disruptor;

import java.util.List;
import java.util.Vector;

import com.lmax.disruptor.EventHandler;

/**
 * @author 释慧利
 */
public abstract class AbstractBatchEventHandler<E extends DisruptorEvent> implements EventHandler<E> {

    private int batchSize;
    private final List<E> tasks;

    public AbstractBatchEventHandler(int batchSize) {
        this.batchSize = batchSize;
        tasks = new Vector<>(this.batchSize);

    }

    protected abstract void handle(List<E> events);

    public void reset() {
        tasks.clear();
    }

    @Override
    public void onEvent(E event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getShutdownLatch() != null) {
            if (!this.tasks.isEmpty()) {
                handle(this.tasks);
                reset();
            }
            event.getShutdownLatch().countDown();
            return;
        }
        this.tasks.add(event);

        if (this.tasks.size() >= batchSize || endOfBatch) {
            handle(this.tasks);
            reset();
        }
    }
}
