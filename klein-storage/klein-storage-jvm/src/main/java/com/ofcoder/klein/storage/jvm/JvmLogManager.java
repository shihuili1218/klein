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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;

/**
 * @author 释慧利
 */
@Join
public class JvmLogManager implements LogManager {

    private ConcurrentMap<Long, Instance> instances;
    private ReentrantReadWriteLock lock;

    @Override
    public void init(StorageProp op) {
        instances = new ConcurrentHashMap<>();
        lock = new ReentrantReadWriteLock(true);
    }

    @Override
    public void shutdown() {
        instances.clear();
    }

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    @Override
    public Instance getInstance(long id) {
        if (!instances.containsKey(id)) {
            return null;
        }
        return instances.get(id);
    }

    @Override
    public void updateInstance(Instance instance) {
        if (!lock.isWriteLockedByCurrentThread()) {
            throw new LockException("before calling this method: updateInstance, you need to obtain the lock");
        }
        instances.put(instance.getInstanceId(), instance);
    }

    @Override
    public long maxInstanceId() {
        return instances.keySet().stream().max(Long::compareTo).get();
    }
}
