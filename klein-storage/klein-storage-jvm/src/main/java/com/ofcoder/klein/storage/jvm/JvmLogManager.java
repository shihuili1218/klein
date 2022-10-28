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
package com.ofcoder.klein.storage.jvm;

import com.google.common.collect.Lists;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author 释慧利
 */
@Join
public class JvmLogManager implements LogManager {

    private ConcurrentMap<Long, Instance> runningInstances;
    private ConcurrentMap<Long, Instance> confirmedInstances;
    private ReentrantReadWriteLock lock;
    private long maxConfirmInstanceId = 0;

    @Override
    public void init(StorageProp op) {
        runningInstances = new ConcurrentHashMap<>();
        confirmedInstances = new ConcurrentHashMap<>();
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public void shutdown() {
        runningInstances.clear();
        confirmedInstances.clear();
    }

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    @Override
    public Instance getInstance(long id) {
        if (runningInstances.containsKey(id)) {
            return runningInstances.get(id);
        }
        if (confirmedInstances.containsKey(id)) {
            return confirmedInstances.get(id);
        }
        return null;
    }

    @Override
    public List<Instance> getInstanceNoConfirm() {
        return Lists.newArrayList(runningInstances.values());
    }

    @Override
    public void updateInstance(Instance instance) {
        if (!lock.isWriteLockedByCurrentThread()) {
            throw new LockException("before calling this method: updateInstance, you need to obtain the lock");
        }
        if (instance.getState() == Instance.State.CONFIRMED) {
            confirmedInstances.put(instance.getInstanceId(), instance);
            runningInstances.remove(instance.getInstanceId());
            if (instance.getInstanceId() < maxConfirmInstanceId) {
                maxConfirmInstanceId = instance.getInstanceId();
            }
        } else {
            runningInstances.put(instance.getInstanceId(), instance);
        }
    }

    @Override
    public long maxInstanceId() {
        long running = runningInstances.keySet().stream().max(Long::compareTo).orElse(0L);
        long confirmed = confirmedInstances.keySet().stream().max(Long::compareTo).orElse(0L);
        return Math.max(running, confirmed);
    }

    @Override
    public long maxProposalNo() {
        return 0;
    }

    @Override
    public long maxAppliedInstanceId() {
        return maxConfirmInstanceId;
    }
}
