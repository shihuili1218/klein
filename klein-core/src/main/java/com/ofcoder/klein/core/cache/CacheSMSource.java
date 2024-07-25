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

import com.ofcoder.klein.common.util.TrueTime;
import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Cache SM.
 *
 * @author 释慧利
 */
public class CacheSMSource implements KleinCache {
    private final CacheContainer container;
    private final CacheProp cacheProp;

    public CacheSMSource(final CacheProp cacheProp) {
        this.cacheProp = cacheProp;
        String temp = this.cacheProp.getDataPath() + File.separator + cacheProp.getId() + File.separator + "temp";
        File file = new File(temp);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            // do nothing for mkdir result.
        }
        if (cacheProp.getLru()) {
            this.container = new LruCacheContainer(cacheProp.getMemorySize(),
                temp + File.separator + "klein-cache.mdb" + "." + System.currentTimeMillis());
        } else {
            this.container = new MemoryCacheContainer();
        }
    }

    @Override
    public boolean exist(final String key) {
        return container.containsKey(key);
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data) {
        container.put(key, data, 0);
        return true;
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data, final boolean apply) {
        container.put(key, data, 0);
        return true;
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data, final Long ttl, final TimeUnit unit) {
        container.put(key, data, 0);
        return true;
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data) {
        return (D) container.putIfAbsent(key, data, 0);
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data, final Long ttl, final TimeUnit unit) {
        return (D) container.putIfAbsent(key, data, TrueTime.currentTimeMillis() + unit.toMillis(ttl));
    }

    @Override
    public <D extends Serializable> D get(final String key) {
        return (D) container.get(key);
    }

    @Override
    public void invalidate(final String key) {
        container.remove(key);
    }

    @Override
    public void invalidateAll() {
        container.clear();
    }
}
