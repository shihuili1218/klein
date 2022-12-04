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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.sm.AbstractSM;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.CacheManager;

/**
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    public static final String GROUP = "cache";

    private static final LRUMap CONTAINER = new LRUMap(3);
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);

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
                CONTAINER.put(message.getKey(), message.getData(), message.getExpire());
                break;
            case Message.GET:
                return CONTAINER.get(message.getKey());
            case Message.INVALIDATE:
                CONTAINER.remove(message.getKey());
                break;
            case Message.INVALIDATEALL:
                CONTAINER.clear();
                break;
            case Message.PUTIFPRESENT:
                return CONTAINER.putIfAbsent(message.getKey(), message.getData(), message.getExpire());
            case Message.EXIST:
                return CONTAINER.exist(message.getKey());
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public Object makeImage() {
        return CONTAINER.makeImage();
    }

    @Override
    public void loadImage(Object snap) {
        CONTAINER.clear();
        CONTAINER.loadImage(snap);
    }

    protected static class LRUMap {
        private final MemoryMap<String, CacheManager.MateData> memory;
        private final CacheManager cacheManager;

        public LRUMap(int size) {
            memory = new MemoryMap<>(size);
            cacheManager = ExtensionLoader.getExtensionLoader(CacheManager.class).getJoin();
        }

        private boolean checkExpire(String key, CacheManager.MateData mateData) {
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

        public boolean exist(String key) {
            if (memory.containsKey(key)) {
                CacheManager.MateData mateData = memory.get(key);
                return checkExpire(key, mateData);
            } else {
                CacheManager.MateData mateData = cacheManager.get(key);
                if (mateData != null) {
                    return checkExpire(key, mateData);
                } else {
                    return false;
                }
            }
        }

        public Object get(String key) {
            CacheManager.MateData mateData;
            if (memory.containsKey(key)) {
                mateData = memory.get(key);
            } else {
                mateData = cacheManager.get(key);
                memory.put(key, mateData);
            }
            if (checkExpire(key, mateData)) {
                return mateData.getData();
            } else {
                return null;
            }
        }

        public synchronized void put(String key, Serializable data) {
            CacheManager.MateData value = new CacheManager.MateData();
            value.setExpire(Message.TTL_PERPETUITY);
            value.setData(data);

            cacheManager.put(key, value);
            memory.put(key, value);
        }


        public synchronized void put(String key, Serializable data, long expire) {
            CacheManager.MateData value = new CacheManager.MateData();
            value.setExpire(expire);
            value.setData(data);

            cacheManager.put(key, value);
            memory.put(key, value);
        }

        public synchronized Object putIfAbsent(String key, Serializable data, long expire) {
            CacheManager.MateData value = new CacheManager.MateData();
            value.setExpire(expire);
            value.setData(data);
            CacheManager.MateData mateData = cacheManager.putIfAbsent(key, value);
            if (mateData == null) {
                memory.put(key, value);
                return null;
            }
            return mateData.getData();
        }

        public synchronized void remove(String key) {
            cacheManager.remove(key);
            memory.remove(key);
        }

        public synchronized void clear() {
            cacheManager.clear();
            memory.clear();
        }

        public Object makeImage() {
            return cacheManager.makeImage();
        }

        public synchronized void loadImage(Object image) {
            cacheManager.loadImage(image);
        }
    }

    private static class MemoryMap<K, V> extends LinkedHashMap<K, V> {
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
