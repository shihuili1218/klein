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
import java.util.HashMap;
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
 * Cache SM.
 *
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    public static final String GROUP = "cache";
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);

    private final LRUMap container;
    private final CacheProp cacheProp;

    public CacheSM(final CacheProp cacheProp) {
        this.cacheProp = cacheProp;
        this.container = new LRUMap(cacheProp.getMemorySize(), cacheProp.getDataPath() + "." + cacheProp.getId());
    }

    @Override
    public Object apply(final Object data) {
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
    public void loadImage(final Object snap) {
        container.clear();
        container.loadImage(snap);
    }

    @Override
    public void close() {
        container.close();
    }

    protected static class LRUMap {
        private final MemoryMap<String, MetaData> memory;
        private final ConcurrentMap<String, MetaData> file;
        private final DB db;

        public LRUMap(final int size, final String dataPath) {
            memory = new MemoryMap<>(size);
            db = DBMaker.fileDB(dataPath).make();
            this.file = db.hashMap(dataPath, Serializer.STRING, new Serializer<MetaData>() {
                @Override
                public void serialize(@NotNull final DataOutput2 out, @NotNull final MetaData value) throws IOException {
                    out.write(Hessian2Util.serialize(value));
                }

                @Override
                public MetaData deserialize(@NotNull final DataInput2 input, final int available) throws IOException {
                    return Hessian2Util.deserialize(input.internalByteArray());
                }
            }).createOrOpen();

        }

        private boolean checkExpire(final String key, final MetaData metaData) {
            if (metaData == null) {
                return false;
            }
            if (metaData.getExpire() == Message.TTL_PERPETUITY) {
                return true;
            }
            if (metaData.getExpire() < System.nanoTime()) {
                remove(key);
                return false;
            } else {
                return true;
            }
        }

        private MetaData getValueFormMemberOrFile(final String key) {
            MetaData metaData = null;
            if (memory.containsKey(key)) {
                metaData = memory.get(key);
            }
            if (metaData == null && file.containsKey(key)) {
                metaData = file.get(key);
                memory.put(key, metaData);
            }
            return metaData;
        }

        private boolean exist(final String key) {
            MetaData metaData = getValueFormMemberOrFile(key);
            return checkExpire(key, metaData);
        }

        private Object get(final String key) {
            MetaData metaData = getValueFormMemberOrFile(key);
            if (checkExpire(key, metaData)) {
                return metaData.getData();
            } else {
                return null;
            }
        }

        private synchronized void put(final String key, final Serializable data) {
            MetaData value = new MetaData();
            value.setExpire(Message.TTL_PERPETUITY);
            value.setData(data);

            file.put(key, value);
            memory.put(key, value);
        }

        private synchronized void put(final String key, final Serializable data, final long expire) {
            MetaData value = new MetaData();
            value.setExpire(expire);
            value.setData(data);

            file.put(key, value);
            memory.put(key, value);
        }

        private synchronized Object putIfAbsent(final String key, final Serializable data, final long expire) {
            MetaData value = new MetaData();
            value.setExpire(expire);
            value.setData(data);
            MetaData metaData = file.putIfAbsent(key, value);
            if (metaData == null) {
                memory.put(key, value);
                return null;
            }
            return metaData.getData();
        }

        private synchronized void remove(final String key) {
            file.remove(key);
            memory.remove(key);
        }

        private synchronized void clear() {
            file.clear();
            memory.clear();
        }

        private Object makeImage() {
            return new HashMap<>(file);
        }

        private synchronized void loadImage(final Object image) {
            if (!(image instanceof Map)) {
                return;
            }
            clear();
            Map<? extends String, ? extends MetaData> snap = (Map<? extends String, ? extends MetaData>) image;
            file.putAll(snap);
            memory.putAll(snap);
        }

        private void close() {
            db.close();
        }
    }

    protected static class MemoryMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public MemoryMap(final int initialCapacity) {
            super(initialCapacity, 0.75f, true);
            this.capacity = initialCapacity;
        }

        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}
