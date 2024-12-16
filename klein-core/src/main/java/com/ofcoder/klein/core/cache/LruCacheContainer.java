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

import com.ofcoder.klein.serializer.hessian2.Hessian2Util;

/**
 * lru cache.
 * if the cached data exceeds the memory size(set klein.cache.lru.memory-size), the oldest data is saved to disk
 *
 * @author 释慧利
 */
public class LruCacheContainer extends ClearExpiryCacheContainer {
    private final MemoryMap<String, MetaData> memory;
    private final ConcurrentMap<String, MetaData> file;
    private final DB db;

    public LruCacheContainer(final int size, final String dataPath) {
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

    @Override
    public boolean containsKey(final String key) {
        MetaData metaData = getValueFormMemberOrFile(key);
        return checkExpire(key, metaData);
    }

    @Override
    public Object get(final String key) {
        MetaData metaData = getValueFormMemberOrFile(key);
        if (checkExpire(key, metaData)) {
            return metaData.getData();
        }
        return null;
    }

    @Override
    public synchronized Object _put(final String key, final Object data, final long expire) {
        MetaData value = new MetaData();
        value.setExpire(expire);
        value.setData(data);

        MetaData metaData = file.put(key, value);
        memory.put(key, value);

        if (checkExpire(key, metaData)) {
            return metaData.getData();
        }
        return null;
    }

    @Override
    public synchronized Object _putIfAbsent(final String key, final Object data, final long expire) {
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
    public Map<String, MetaData> _makeImage() {
        return new HashMap<>(file);
    }

    @Override
    public synchronized void _loadImage(final Map<String, MetaData> image) {
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
