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
import java.util.concurrent.TimeUnit;

import com.ofcoder.klein.common.OnlyForTest;
import com.ofcoder.klein.consensus.facade.Result;

/**
 * Klein Cache.
 *
 * @author 释慧利
 */
public interface KleinCache {
    /**
     * check key is exist.
     *
     * @param key check key.
     * @return check result
     */
    boolean exist(String key);

    /**
     * put element to cache.
     *
     * @param key  cache key
     * @param data cache value
     * @param <D>  cache value type
     * @return put result
     */
    <D extends Serializable> boolean put(String key, D data);

    /**
     * only for jepsen test.
     *
     * @param key   cache key
     * @param data  cache value
     * @param <D>   cache value type
     * @param apply if ture, wait sm apply before return
     * @param ttl  expire
     * @param unit expire unit
     * @return put result
     */
    @OnlyForTest("for jepsen")
    <D extends Serializable> Result.State put(String key, D data, boolean apply, Long ttl, TimeUnit unit);

    /**
     * put element to cache and set expire.
     *
     * @param key  cache key
     * @param data cache value
     * @param ttl  expire
     * @param unit expire unit
     * @param <D>  cache value type
     * @return put result
     */
    <D extends Serializable> boolean put(String key, D data, Long ttl, TimeUnit unit);

    /**
     * put element to cache if present.
     *
     * @param key  cache key
     * @param data cache value
     * @param <D>  cache value type
     * @return cache value
     */
    <D extends Serializable> D putIfPresent(String key, D data);

    /**
     * put element to cache if present, and set expire.
     *
     * @param key  cache key
     * @param data cache value
     * @param <D>  cache value type
     * @param ttl  expire
     * @param unit expire unit
     * @return cache value
     */
    <D extends Serializable> D putIfPresent(String key, D data, Long ttl, TimeUnit unit);

    /**
     * get element by key from cache.
     *
     * @param key cache key
     * @param <D> cache value type
     * @return cache value
     */
    <D extends Serializable> D get(String key);

    /**
     * remove key from cache.
     *
     * @param key remove key
     */
    void invalidate(String key);

    /**
     * clear cache.
     */
    void invalidateAll();

}
