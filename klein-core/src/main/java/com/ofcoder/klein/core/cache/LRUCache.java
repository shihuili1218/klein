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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author 释慧利
 */
public class LRUCache {
    private final MemoryMap<String, Object> memory;

    public LRUCache(int size) {
        memory = new MemoryMap<>(size);
    }


    private class MemoryMap<K, V> extends LinkedHashMap<K, V> {
        private int capacity;

        public MemoryMap(int initialCapacity) {
            super(initialCapacity, 0.75f, true);
            this.capacity = initialCapacity;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }

        @Override
        public V get(Object key) {
            return super.get(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return super.containsValue(value);
        }

        @Override
        public V put(K key, V value) {
            return super.put(key, value);
        }
    }
}