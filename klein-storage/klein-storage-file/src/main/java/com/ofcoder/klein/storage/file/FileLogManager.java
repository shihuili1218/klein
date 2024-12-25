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
package com.ofcoder.klein.storage.file;

import com.ofcoder.klein.common.util.StreamUtil;
import com.ofcoder.klein.spi.Join;
import com.ofcoder.klein.storage.facade.Instance;
import com.ofcoder.klein.storage.facade.LogManager;
import com.ofcoder.klein.storage.facade.Snap;
import com.ofcoder.klein.storage.facade.config.StorageProp;
import com.ofcoder.klein.storage.facade.exception.LockException;
import com.ofcoder.klein.storage.facade.exception.StorageException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jvm LogManager.
 *
 * @author 释慧利
 */
@Join
public class FileLogManager<P extends Serializable> implements LogManager<P> {
    private static final Logger LOG = LoggerFactory.getLogger(FileLogManager.class);

    private static String selfPath;
    private static String metaPath;

    private ConcurrentMap<Long, Instance<P>> runningInstances;
    private ConcurrentMap<Long, Instance<P>> confirmedInstances;
    private ConcurrentMap<Long, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private byte[] metadata;

    public FileLogManager(final StorageProp op) {
        runningInstances = new ConcurrentHashMap<>();
        confirmedInstances = new ConcurrentHashMap<>();

        selfPath = op.getDataPath();
        File selfFile = new File(selfPath);
        if (!selfFile.exists()) {
            boolean ignored = selfFile.mkdirs();
            // do nothing for mkdir result
        }

        metaPath = selfPath + File.separator + "mate";
    }

    @Override
    public void shutdown() {
        runningInstances.clear();
        confirmedInstances.clear();
    }

    @Override
    public ReentrantReadWriteLock getLock(final long instanceId) {
        locks.putIfAbsent(instanceId, new ReentrantReadWriteLock(true));
        return locks.get(instanceId);
    }

    @Override
    public Instance<P> getInstance(final long id) {
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
    public List<Instance<P>> getInstanceConfirmed() {
        return new ArrayList<>(confirmedInstances.values());
    }

    @Override
    public void updateInstance(final Instance<P> instance) {
        ReentrantReadWriteLock lock = locks.get(instance.getInstanceId());
        if (lock == null || !lock.isWriteLockedByCurrentThread()) {
            throw new LockException("before calling this method: updateInstance, you need to obtain the lock");
        }
        if (instance.getState() == Instance.State.CONFIRMED) {
            confirmedInstances.put(instance.getInstanceId(), instance);
            runningInstances.remove(instance.getInstanceId());
        } else {
            runningInstances.put(instance.getInstanceId(), instance);
        }
        saveMetaData();
    }

    @Override
    public byte[] loadMetaData() {
        File file = new File(metaPath);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream lastIn = new FileInputStream(file);) {
            this.metadata = IOUtils.toByteArray(lastIn);
            return this.metadata;
        } catch (IOException e) {
            throw new StorageException("loadMetaData, " + e.getMessage(), e);
        }
    }

    private void saveMetaData() {
        FileOutputStream mateOut = null;
        try {
            mateOut = new FileOutputStream(metaPath);
            IOUtils.write(this.metadata, mateOut);
        } catch (IOException e) {
            throw new StorageException("save snap, " + e.getMessage(), e);
        } finally {
            StreamUtil.close(mateOut);
        }
    }

    @Override
    public void saveSnap(final String group, final Snap snap) {
        LOG.info("save snap, group: {}, checkpoint: {}", group, snap.getCheckpoint());
        String bastPath = selfPath + File.separator + group + File.separator;
        File snapFile = new File(bastPath + snap.getCheckpoint());
        if (snapFile.exists()) {
            return;
        }
        File baseDir = new File(bastPath);
        if (!baseDir.exists()) {
            boolean ignored = baseDir.mkdirs();
        }

        File lastFile = new File(bastPath + "last");

        try (FileOutputStream snapOut = new FileOutputStream(snapFile);
             FileOutputStream lastOut = new FileOutputStream(lastFile)) {
            IOUtils.write(snap.getSnap(), snapOut);
            IOUtils.write(snapFile.getPath(), lastOut, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("save snap, " + e.getMessage(), e);
        }

        truncCheckpoint(snap.getCheckpoint());
        saveMetaData();
    }

    private void truncCheckpoint(final long checkpoint) {
        Set<Long> removeKeys = confirmedInstances.keySet().stream().filter(it -> it <= checkpoint).collect(Collectors.toSet());
        removeKeys.forEach(confirmedInstances::remove);
    }

    @Override
    public Snap getLastSnap(final String group) {
        String bastPath = selfPath + File.separator + group + File.separator;
        File file = new File(bastPath + "last");
        if (!file.exists()) {
            return null;
        }

        Snap lastSnap;
        try (FileInputStream lastIn = new FileInputStream(file)) {
            String snapFile = IOUtils.toString(lastIn, StandardCharsets.UTF_8);
            String checkpointString = snapFile.substring(snapFile.lastIndexOf(File.separator) + 1);
            LOG.info("get snap, group: {}, checkpoint: {}", group, checkpointString);
            long checkpoint = Long.parseLong(checkpointString);

            try (FileInputStream snapIn = new FileInputStream(snapFile)) {
                lastSnap = new Snap(checkpoint, IOUtils.toByteArray(snapIn));
                return lastSnap;
            }
        } catch (NumberFormatException | IOException e) {
            throw new StorageException("get last snap, " + e.getMessage(), e);
        }
    }

}
