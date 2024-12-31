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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.ofcoder.klein.serializer.Serializer;
import com.ofcoder.klein.spi.ExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.sm.AbstractSM;

/**
 * Cache SM.
 *
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    public static final String GROUP = "cache";
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);

    private final Map<String, CacheContainer> containers = new HashMap<>();
    private final CacheProp cacheProp;
    private final String temp;
    private final Serializer serializer;

    public CacheSM(final CacheProp cacheProp) {
        this.serializer = ExtensionLoader.getExtensionLoader(Serializer.class).register("hessian2");
        this.cacheProp = cacheProp;
        this.temp = this.cacheProp.getDataPath() + File.separator + cacheProp.getId() + File.separator + "temp";
        File file = new File(temp);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            // do nothing for mkdir result.
        }
    }

    private CacheContainer getCacheContainer(final String cacheName) {
        if (cacheProp.getLru()) {
            containers.putIfAbsent(cacheName, new LruCacheContainer(cacheProp.getMemorySize(),
                    temp + File.separator + "klein-cache.mdb" + "." + System.currentTimeMillis()));
        } else {
            containers.putIfAbsent(cacheName, new MemoryCacheContainer());
        }
        return containers.get(cacheName);
    }

    @Override
    public byte[] apply(final byte[] original) {
        Object data = serializer.deserialize(original);

        if (!(data instanceof CacheMessage)) {
            LOG.warn("apply data, UNKNOWN PARAMETER TYPE, data type is {}", data.getClass().getName());
            return null;
        }
        CacheMessage message = (CacheMessage) data;
        CacheContainer container = getCacheContainer(message.getCacheName());
        Object result;
        switch (message.getOp()) {
            case CacheMessage.PUT:
                container.put(message.getKey(), message.getData(), message.getExpire());
                break;
            case CacheMessage.GET:
                result = container.get(message.getKey());
                return serializer.serialize(result);
            case CacheMessage.INVALIDATE:
                container.remove(message.getKey());
                break;
            case CacheMessage.INVALIDATEALL:
                container.clear();
                break;
            case CacheMessage.PUTIFPRESENT:
                result = container.putIfAbsent(message.getKey(), message.getData(), message.getExpire());
                return serializer.serialize(result);
            case CacheMessage.EXIST:
                result = container.containsKey(message.getKey());
                return serializer.serialize(result);
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public byte[] makeImage() {
        Map<String, CacheSnap> result = new HashMap<>();
        containers.forEach((cacheName, container) -> result.put(cacheName, container.makeImage()));
        return serializer.serialize(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadImage(final byte[] original) {
        Object snap = serializer.deserialize(original);
        if (!(snap instanceof Map)) {
            return;
        }
        Map<String, CacheSnap> snapCaches = (Map<String, CacheSnap>) snap;
        containers.clear();
        snapCaches.forEach((cacheName, cacheSnap) -> getCacheContainer(cacheName).loadImage(cacheSnap));
    }

    @Override
    public void close() {
        containers.values().forEach(CacheContainer::close);
    }
}
