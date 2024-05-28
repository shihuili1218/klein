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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * memory cache.
 * all data will be stored in memory.
 *
 * @author 释慧利
 */
public class MemoryCacheContainer extends ClearExpiryCacheContainer {

    private final ConcurrentMap<String, MetaData> memory = new ConcurrentHashMap<>();

    @Override
    public boolean containsKey(final String key) {
        MetaData result = memory.getOrDefault(key, null);
        return checkExpire(key, result);
    }

    @Override
    public Object get(final String key) {
        MetaData result = memory.getOrDefault(key, null);
        if (checkExpire(key, result)) {
            return result.getData();
        }
        return null;
    }

    @Override
    public Object _put(final String key, final Object data, final long expire) {
        MetaData value = new MetaData();
        value.setExpire(expire);
        value.setData(data);
        MetaData put = memory.put(key, value);

        if (checkExpire(key, put)) {
            return put.getData();
        }
        return null;
    }

    @Override
    public Object _putIfAbsent(final String key, final Object data, final long expire) {
        MetaData value = new MetaData();
        value.setExpire(expire);
        value.setData(data);
        MetaData pre = memory.putIfAbsent(key, value);

        if (checkExpire(key, pre)) {
            return pre.getData();
        }
        return null;
    }

    @Override
    public void remove(final String key) {
        memory.remove(key);
    }

    @Override
    public void clear() {
        memory.clear();
    }

    @Override
    public Map<String, MetaData> _makeImage() {
        return new HashMap<>(memory);
    }

    @Override
    public void _loadImage(final Map<String, MetaData> image) {
        memory.clear();
        memory.putAll(image);
    }

    @Override
    public void close() {

    }

}
