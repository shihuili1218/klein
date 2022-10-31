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
package com.ofcoder.klein.consensus.facade;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.ofcoder.klein.consensus.facade.exception.StateMachineException;
import com.ofcoder.klein.storage.facade.Snap;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * @author: 释慧利
 */
public abstract class AbstractSM implements SM {
    private static final AtomicBoolean SNAP = new AtomicBoolean(false);
    private static final ReentrantLock SNAP_LOCK = new ReentrantLock(true);
    private static Long checkpoint = 0L;

    @Override
    public Object apply(long instanceId, Object data) {
        if (SNAP.get()) {
            try {
                SNAP_LOCK.lock();
                checkpoint = Math.max(instanceId, checkpoint);
                return apply(data);
            } finally {
                SNAP_LOCK.unlock();
            }
        } else {
            checkpoint = Math.max(instanceId, checkpoint);
            return apply(data);
        }
    }

    protected abstract Object apply(Object data);

    public Snap snapshot() {
        if (SNAP.compareAndSet(false, true)) {
            try {
                SNAP_LOCK.lock();
                return new Snap(checkpoint, makeImage());
            } catch (Exception e) {
                throw new StateMachineException("Create snapshot failure, " + e.getMessage(), e);
            } finally {
                SNAP.compareAndSet(true, false);
                SNAP_LOCK.unlock();
            }
        } else {
            return null;
        }
    }

    protected abstract Object makeImage();

    @Override
    public void loadSnap(Snap snap) {
        if (SNAP.compareAndSet(false, true)) {
            try {
                SNAP_LOCK.lock();
                checkpoint = snap.getCheckpoint();
                loadImage(snap.getSnap());
            } catch (Exception e) {
                throw new StateMachineException("Load snapshot failure, " + e.getMessage(), e);
            } finally {
                SNAP.compareAndSet(true, false);
                SNAP_LOCK.unlock();
            }
        }
    }

    protected abstract void loadImage(Object snap);

}

