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
package com.ofcoder.klein.core.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.serialization.Hessian2Util;
import com.ofcoder.klein.consensus.facade.sm.AbstractSM;
import com.ofcoder.klein.core.config.CacheProp;

/**
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);
    public static final String GROUP = "cache";

    private final LRUMap container;
    private final CacheProp cacheProp;

    public CacheSM(CacheProp cacheProp) {
        this.cacheProp = cacheProp;
        this.container = new LRUMap(cacheProp.getMemorySize(), cacheProp.getDataPath() + cacheProp.getId());
    }

    @Override
    public Object apply(Object data) {
        LOG.info("apply data: {}", data);
        if (!(data instanceof Message)) {
            LOG.warn("apply data, UNKNOWN PARAMETER TYPE, data type is {}", data.getClass().getName());
            return null;
        }
        Message message = (Message) data;
        switch (message.getOp()) {
            case Message.PUT:
                container.put(message.getKey(), message.getData(), message.getExpire());
                break;
            case Message.GET:
                return container.get(message.getKey());
            case Message.INVALIDATE:
                container.remove(message.getKey());
                break;
            case Message.INVALIDATEALL:
                container.clear();
                break;
            case Message.PUTIFPRESENT:
                return container.putIfAbsent(message.getKey(), message.getData(), message.getExpire());
            case Message.EXIST:
                return container.exist(message.getKey());
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public Object makeImage() {
        return container.makeImage();
    }

    @Override
    public void loadImage(Object snap) {
        container.clear();
        container.loadImage(snap);
    }

    protected static class LRUMap {
        private final MemoryMap<String, MateData> memory;
        private final ConcurrentMap<String, MateData> file;

        public LRUMap(int size, String dataPath) {
            memory = new MemoryMap<>(size);
            DB db = DBMaker.fileDB(dataPath).closeOnJvmShutdown().make();
            this.file = db.hashMap(dataPath, Serializer.STRING, new Serializer<MateData>() {
                @Override
                public void serialize(@NotNull DataOutput2 out, @NotNull MateData value) throws IOException {
                    out.write(Hessian2Util.serialize(value));
                }

                @Override
                public MateData deserialize(@NotNull DataInput2 input, int available) throws IOException {
                    return Hessian2Util.deserialize(input.internalByteArray());
                }
            }).createOrOpen();

        }

        private boolean checkExpire(String key, MateData mateData) {
            if (mateData == null) {
                return false;
            }
            if (mateData.getExpire() == Message.TTL_PERPETUITY) {
                return true;
            }
            if (mateData.getExpire() < System.nanoTime()) {
                remove(key);
                return false;
            } else {
                return true;
            }
        }

        private MateData getValueFormMemberOrFile(String key) {
            MateData mateData = null;
            if (memory.containsKey(key)) {
                mateData = memory.get(key);
            }
            if (mateData == null && file.containsKey(key)) {
                mateData = file.get(key);
                memory.put(key, mateData);
            }
            return mateData;
        }

        public boolean exist(String key) {
            MateData mateData = getValueFormMemberOrFile(key);
            return checkExpire(key, mateData);
        }

        public Object get(String key) {
            MateData mateData = getValueFormMemberOrFile(key);
            if (checkExpire(key, mateData)) {
                return mateData.getData();
            } else {
                return null;
            }
        }

        public synchronized void put(String key, Serializable data) {
            MateData value = new MateData();
            value.setExpire(Message.TTL_PERPETUITY);
            value.setData(data);

            file.put(key, value);
            memory.put(key, value);
        }

        public synchronized void put(String key, Serializable data, long expire) {
            MateData value = new MateData();
            value.setExpire(expire);
            value.setData(data);

            file.put(key, value);
            memory.put(key, value);
        }

        public synchronized Object putIfAbsent(String key, Serializable data, long expire) {
            MateData value = new MateData();
            value.setExpire(expire);
            value.setData(data);
            MateData mateData = file.putIfAbsent(key, value);
            if (mateData == null) {
                memory.put(key, value);
                return null;
            }
            return mateData.getData();
        }

        public synchronized void remove(String key) {
            file.remove(key);
            memory.remove(key);
        }

        public synchronized void clear() {
            file.clear();
            memory.clear();
        }

        public Object makeImage() {
            return file;
        }

        public synchronized void loadImage(Object image) {
            if (!(image instanceof Map)) {
                return;
            }
            clear();
            Map<? extends String, ? extends MateData> snap = (Map<? extends String, ? extends MateData>) image;
            file.putAll(snap);
            memory.putAll(snap);
        }
    }

    protected static class MemoryMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public MemoryMap(int initialCapacity) {
            super(initialCapacity, 0.75f, true);
            this.capacity = initialCapacity;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}
