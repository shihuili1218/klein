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

import java.util.concurrent.locks.ReentrantLock;

import com.ofcoder.klein.consensus.facade.exception.StateMachineException;
import com.ofcoder.klein.storage.facade.Snap;

/**
 * Add Snapshot and Checkpoint in SM.
 *
 * @author 释慧利
 */
public abstract class AbstractSM implements SM {
    private static final ReentrantLock APPLYING_LOCK = new ReentrantLock(true);
    private static Long lastAppliedId = 0L;

    @Override
    public long lastAppliedId() {
        return lastAppliedId;
    }

    @Override
    public Object apply(final long instanceId, final Object data) {
        if (lastAppliedId >= instanceId) {
            throw new StateMachineException(String.format("instance[%s] apply sm, but the instance has bean applied", instanceId));
        }
        try {
            APPLYING_LOCK.lock();
            lastAppliedId = Math.max(instanceId, lastAppliedId);
            return apply(data);
        } finally {
            APPLYING_LOCK.unlock();
        }
    }

    /**
     * apply instance.
     *
     * @param data proposal's data
     * @return apply result
     */
    protected abstract Object apply(Object data);

    @Override
    public Snap snapshot() {
        try {
            APPLYING_LOCK.lock();
            return new Snap(lastAppliedId, makeImage());
        } catch (Exception e) {
            throw new StateMachineException("Create snapshot failure, " + e.getMessage(), e);
        } finally {
            APPLYING_LOCK.unlock();
        }
    }

    /**
     * take a photo.
     *
     * @return image
     */
    protected abstract Object makeImage();

    @Override
    public void loadSnap(final Snap snap) {
        if (snap == null) {
            return;
        }
        try {
            APPLYING_LOCK.lock();
            lastAppliedId = snap.getCheckpoint();
            loadImage(snap.getSnap());
        } catch (Exception e) {
            throw new StateMachineException("Load snapshot failure, " + e.getMessage(), e);
        } finally {
            APPLYING_LOCK.unlock();
        }
    }

    /**
     * load image.
     *
     * @param snap image
     */
    protected abstract void loadImage(Object snap);

    @Override
    public void close() {

    }
}

