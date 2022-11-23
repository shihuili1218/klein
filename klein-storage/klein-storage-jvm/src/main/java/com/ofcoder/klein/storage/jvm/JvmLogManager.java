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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.MateData;
import com.ofcoder.klein.storage.facade.Snap;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;
import com.ofcoder.klein.storage.facade.exception.StorageException;

/**
 * @author 释慧利
 */
@Join
public class JvmLogManager<P extends Serializable> implements LogManager<P> {

    private ConcurrentMap<Long, Instance<P>> runningInstances;
    private ConcurrentMap<Long, Instance<P>> confirmedInstances;
    private ReentrantReadWriteLock lock;
    private static final String BASE_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "data";
    private static String SELF_PATH;
    private static String MATE_PATH;

    private MateData metadata;

    @Override
    public void init(StorageProp op) {
        File file = new File(BASE_PATH);
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
        }

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
    public Instance<P> getInstance(long id) {
        if (runningInstances.containsKey(id)) {
            return runningInstances.get(id);
        }
        if (confirmedInstances.containsKey(id)) {
            return confirmedInstances.get(id);
        }
        return null;
    }

    @Override
    public List<Instance<P>> getInstanceNoConfirm() {
        return new ArrayList<>(runningInstances.values());
    }

    @Override
    public void updateInstance(Instance<P> instance) {
        if (!lock.isWriteLockedByCurrentThread()) {
            throw new LockException("before calling this method: updateInstance, you need to obtain the lock");
        }
        if (instance.getState() == Instance.State.CONFIRMED) {
            confirmedInstances.put(instance.getInstanceId(), instance);
            runningInstances.remove(instance.getInstanceId());
        } else {
            runningInstances.put(instance.getInstanceId(), instance);
        }
        saveMateData();
    }


    @Override
    public MateData loadMateData(MateData defaultValue) {
        SELF_PATH = BASE_PATH + File.separator + defaultValue.nodeId();
        File selfFile = new File(SELF_PATH);
        if (!selfFile.exists()) {
            boolean mkdir = selfFile.mkdir();
            // do nothing for mkdir result
        }

        MATE_PATH = SELF_PATH + File.separator + "mate";
        File file = new File(MATE_PATH);
        if (!file.exists()) {
            this.metadata = defaultValue;
            return this.metadata;
        }
        FileInputStream lastIn = null;
        try {
            lastIn = new FileInputStream(file);
            this.metadata = Hessian2Util.deserialize(IOUtils.toByteArray(lastIn));
            return this.metadata;
        } catch (IOException e) {
            throw new StorageException("get checkpoint, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(lastIn);
        }
    }

    private void saveMateData() {
        FileOutputStream mateOut = null;
        try {
            mateOut = new FileOutputStream(MATE_PATH);
            IOUtils.write(Hessian2Util.serialize(this.metadata), mateOut);
        } catch (IOException e) {
            throw new StorageException("save snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(mateOut);
        }
    }

    @Override
    public void saveSnap(String group, Snap snap) {
        String bastPath = SELF_PATH + File.separator + group + File.separator;
        File snapFile = new File(bastPath + snap.getCheckpoint());
        if (snapFile.exists()) {
            return;
        }
        File baseDir = new File(bastPath);
        if (!baseDir.exists()) {
            boolean mkdir = baseDir.mkdir();
        }

        File lastFile = new File(bastPath + "last");

        FileOutputStream snapOut = null;
        FileOutputStream lastOut = null;
        try {
            lastOut = new FileOutputStream(lastFile);
            snapOut = new FileOutputStream(snapFile);
            IOUtils.write(Hessian2Util.serialize(snap), snapOut);
            IOUtils.write(Hessian2Util.serialize(snapFile.getPath()), lastOut);
        } catch (IOException e) {
            throw new StorageException("save snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(snapOut);
            StreamUtil.close(lastOut);
        }

        truncCheckpoint(snap.getCheckpoint());
        saveMateData();
    }

    private void truncCheckpoint(long checkpoint) {
        Set<Long> removeKeys = confirmedInstances.keySet().stream().filter(it -> it <= checkpoint).collect(Collectors.toSet());
        removeKeys.forEach(confirmedInstances::remove);
    }

    @Override
    public Snap getLastSnap(String group) {
        String bastPath = SELF_PATH + File.separator + group + File.separator;
        File file = new File(bastPath + "last");
        if (!file.exists()) {
            return null;
        }

        Snap lastSnap;
        FileInputStream lastIn = null;
        FileInputStream snapIn = null;
        try {
            lastIn = new FileInputStream(file);
            String deserialize = Hessian2Util.deserialize(IOUtils.toByteArray(lastIn));
            snapIn = new FileInputStream(deserialize);
            lastSnap = Hessian2Util.deserialize(IOUtils.toByteArray(snapIn));
            return lastSnap;
        } catch (IOException e) {
            throw new StorageException("get last snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(lastIn);
            StreamUtil.close(snapIn);
        }
    }


}
