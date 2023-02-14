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

import com.ofcoder.klein.common.serialization.Hessian2Util;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * lru cache.
 * if the cached data exceeds the memory size(set klein.cache.lru.memory-size), the oldest data is saved to disk
 *
 * @author 释慧利
 */
public class LruCacheContainer<D extends Serializable> implements CacheContainer<D> {
    private final MemoryMap<String, MetaData<D>> memory;
    private final ConcurrentMap<String, MetaData<D>> file;
    private final DB db;

    public LruCacheContainer(final int size, final String dataPath) {
        memory = new MemoryMap<>(size);
        db = DBMaker.fileDB(dataPath).make();
        this.file = db.hashMap(dataPath, Serializer.STRING, new Serializer<MetaData<D>>() {
            @Override
            public void serialize(@NotNull final DataOutput2 out, @NotNull final MetaData<D> value) throws IOException {
                out.write(Hessian2Util.serialize(value));
            }

            @Override
            public MetaData<D> deserialize(@NotNull final DataInput2 input, final int available) throws IOException {
                return Hessian2Util.deserialize(input.internalByteArray());
            }
        }).createOrOpen();
    }

    private MetaData<D> getValueFormMemberOrFile(final String key) {
        MetaData<D> metaData = null;
        if (memory.containsKey(key)) {
            metaData = memory.get(key);
        }
        if (metaData == null && file.containsKey(key)) {
            metaData = file.get(key);
            memory.put(key, metaData);
        }
        return metaData;
    }

    @Override
    public boolean containsKey(final String key) {
        MetaData<D> metaData = getValueFormMemberOrFile(key);
        return checkExpire(key, metaData);
    }

    @Override
    public D get(final String key) {
        MetaData<D> metaData = getValueFormMemberOrFile(key);
        if (checkExpire(key, metaData)) {
            return metaData.getData();
        }
        return null;
    }

    @Override
    public synchronized D put(final String key, final D data, final Long expire) {
        MetaData<D> value = new MetaData<>();
        value.setExpire(expire);
        value.setData(data);

        MetaData<D> metaData = file.put(key, value);
        memory.put(key, value);

        if (checkExpire(key, metaData)) {
            return metaData.getData();
        }
        return null;
    }

    @Override
    public synchronized D putIfAbsent(final String key, final D data, final Long expire) {
        MetaData<D> value = new MetaData<>();
        value.setExpire(expire);
        value.setData(data);

        MetaData<D> metaData = file.putIfAbsent(key, value);
        if (metaData == null) {
            memory.put(key, value);
            return null;
        }
        return metaData.getData();
    }

    @Override
    public synchronized void remove(final String key) {
        file.remove(key);
        memory.remove(key);
    }

    @Override
    public synchronized void clear() {
        file.clear();
        memory.clear();
    }

    @Override
    public Map<String, MetaData<D>> makeImage() {
        return new HashMap<>(file);
    }

    @Override
    public synchronized void loadImage(final Map<String, MetaData<D>> image) {
        clear();
        file.putAll(image);
        memory.putAll(image);
    }

    @Override
    public void close() {
        db.close();
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
