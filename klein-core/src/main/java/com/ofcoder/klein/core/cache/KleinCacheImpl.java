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

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.serializer.hessian2.Hessian2Util;
import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.core.GroupWrapper;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Klein Cache Implement.
 *
 * @author 释慧利
 */
public class KleinCacheImpl implements KleinCache {
    protected GroupWrapper consensus;
    private final String cacheName;

    /**
     * Return a new cache container.
     *
     * @param cacheName cacheName
     */
    public KleinCacheImpl(final String cacheName) {
        this.cacheName = cacheName;
        SMRegistry.register(CacheSM.GROUP, new CacheSM(CacheProp.loadIfPresent()));
        this.consensus = new GroupWrapper(CacheSM.GROUP);
    }

    @Override
    public boolean exist(final String key) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setKey(key);
        message.setOp(CacheMessage.EXIST);
        Result<Boolean> result = consensus.read(message);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        Boolean data = result.getData();
        return data != null && data;
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setData(data);
        message.setKey(key);
        message.setOp(CacheMessage.PUT);
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> Result.State put(final String key, final D data, final boolean apply, final Long ttl, final TimeUnit unit) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setData(data);
        message.setKey(key);
        message.setOp(CacheMessage.PUT);
        if (ttl > 0) {
            message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));
        }
        Result result = consensus.propose(Hessian2Util.serialize(message), apply);
        return result.getState();
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data, final Long ttl, final TimeUnit unit) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setData(data);
        message.setKey(key);
        message.setOp(CacheMessage.PUT);

        message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setData(data);
        message.setKey(key);
        message.setOp(CacheMessage.PUTIFPRESENT);
        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm. key: " + key);
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data, final Long ttl, final TimeUnit unit) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setData(data);
        message.setKey(key);
        message.setOp(CacheMessage.PUTIFPRESENT);
        message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));

        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> D get(final String key) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setKey(key);
        message.setOp(CacheMessage.GET);
        Result<D> result = consensus.read(message);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException(String.format("The consensus negotiation result is %s. In this case, the operation"
                    + " may or may not be completed. You need to retry or query to confirm", result.getState()));
        }
        return result.getData();
    }

    @Override
    public void invalidate(final String key) {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setKey(key);
        message.setOp(CacheMessage.INVALIDATE);
        Result result = consensus.propose(message);
    }

    @Override
    public void invalidateAll() {
        CacheMessage message = new CacheMessage();
        message.setCacheName(cacheName);
        message.setOp(CacheMessage.INVALIDATEALL);
        Result result = consensus.propose(message);
    }

}
