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

import com.ofcoder.klein.consensus.facade.sm.AbstractSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * Cache SM.
 *
 * @author 释慧利
 */
public class CacheSM extends AbstractSM {
    public static final String GROUP = "cache";
    private static final Logger LOG = LoggerFactory.getLogger(CacheSM.class);

    private final CacheContainer<Serializable> container;
    private final CacheProp cacheProp;

    public CacheSM(final CacheProp cacheProp) {
        this.cacheProp = cacheProp;
        String temp = this.cacheProp.getDataPath() + File.separator + cacheProp.getId() + File.separator + "temp";
        File file = new File(temp);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
            // do nothing for mkdir result.
        }
        if (cacheProp.getLru()) {
            this.container = new LruCacheContainer<>(cacheProp.getMemorySize(),
                    temp + File.separator + "klein-cache.mdb" + "." + System.currentTimeMillis());
        } else {
            this.container = new MemoryCacheContainer<>();
        }
    }

    @Override
    public Object apply(final Object data) {
        if (!(data instanceof Message)) {
            LOG.warn("apply data, UNKNOWN PARAMETER TYPE, data type is {}", data.getClass().getName());
            return null;
        }
        Message message = (Message) data;
        switch (message.getOp()) {
            case Message.PUT:
                container.put(message.getKey(), message.getData(), message.getExpire());
                break;
            case Message.GET:
                return container.get(message.getKey());
            case Message.INVALIDATE:
                container.remove(message.getKey());
                break;
            case Message.INVALIDATEALL:
                container.clear();
                break;
            case Message.PUTIFPRESENT:
                return container.putIfAbsent(message.getKey(), message.getData(), message.getExpire());
            case Message.EXIST:
                return container.containsKey(message.getKey());
            default:
                LOG.warn("apply data, UNKNOWN OPERATION, operation type is {}", message.getOp());
                break;
        }
        return null;
    }

    @Override
    public Object makeImage() {
        return container.makeImage();
    }

    @Override
    public void loadImage(final Object snap) {
        if (!(snap instanceof Map)) {
            return;
        }
        container.clear();
        container.loadImage((Map<String, MetaData<Serializable>>) snap);
    }

    @Override
    public void close() {
        container.close();
    }
}
