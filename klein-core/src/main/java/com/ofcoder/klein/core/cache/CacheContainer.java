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

/**
 * Klein Cache.
 *
 * @author 释慧利
 */
public interface CacheContainer {

    /**
     * check key is exist.
     *
     * @param key check key.
     * @return check result
     */
    boolean containsKey(String key);

    /**
     * get element by key from cache.
     *
     * @param key cache key
     * @return cache value
     */
    Object get(String key);

    /**
     * put element to cache and set expire.
     *
     * @param key    cache key
     * @param data   cache value
     * @param expire expire
     * @return the previous value associated with key, or null
     */
    Object put(String key, Object data, long expire);

    /**
     * put element to cache if present, and set expire.
     *
     * @param key    cache key
     * @param data   cache value
     * @param expire expire
     * @return the previous value associated with key, or null
     */
    Object putIfAbsent(String key, Object data, long expire);

    /**
     * remove key from cache.
     *
     * @param key remove key
     */
    void remove(String key);

    /**
     * clear cache.
     */
    void clear();

    /**
     * take a image.
     *
     * @return snapshot.
     */
    CacheSnap makeImage();

    /**
     * load the snapshot to overwrite the data.
     *
     * @param image snapshot
     */
    void loadImage(CacheSnap image);

    /**
     * close.
     */
    void close();

}
