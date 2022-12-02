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
public class LRUMap {
    private final MemoryMap<String, Object> memory;

    public LRUMap(int size) {
        memory = new MemoryMap<>(size, new OverflowListener<String, Object>() {
            @Override
            public void exceed(Map.Entry<String, Object> eldest) {

            }
        });
    }

    public boolean exist(String key) {
        return false;
    }

    public void put(String key, Object data) {

    }

    public Object get(String key) {
        return null;
    }

    public void remove(String key) {

    }

    public void clear() {

    }

    public Object putIfAbsent(String key, Object data) {
        return null;
    }


    public Object makeImage() {

        return null;
    }

    public void loadImage(Object image) {

    }

    private static class MemoryMap<K, V> extends LinkedHashMap<K, V> {
        private int capacity;
        private OverflowListener<K, V> listener;

        public MemoryMap(int initialCapacity, OverflowListener<K, V> listener) {
            super(initialCapacity, 0.75f, true);
            this.capacity = initialCapacity;
            this.listener = listener;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            boolean remove = size() > capacity;
            if (remove) {
                listener.exceed(eldest);
            }
            return remove;
        }
    }


    interface OverflowListener<K, V> {
        void exceed(Map.Entry<K, V> eldest);
    }
}