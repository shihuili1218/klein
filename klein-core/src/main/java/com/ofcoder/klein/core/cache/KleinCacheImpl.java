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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.core.GroupWrapper;

/**
 * Klein Cache Implement.
 *
 * @author 释慧利
 */
public class KleinCacheImpl implements KleinCache {
    private static final Logger LOG = LoggerFactory.getLogger(KleinCacheImpl.class);
    protected GroupWrapper consensus;

    public KleinCacheImpl() {
        this.consensus = new GroupWrapper(CacheSM.GROUP);
    }

    @Override
    public boolean exist(final String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.EXIST);
        Result<Boolean> result = consensus.read(message);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> boolean put(final String key, final D data, final Long ttl, final TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUT);

        message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));
        Result result = consensus.propose(message);
        return Result.State.SUCCESS.equals(result.getState());
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm. key: " + key);
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> D putIfPresent(final String key, final D data, final Long ttl, final TimeUnit unit) {
        Message message = new Message();
        message.setData(data);
        message.setKey(key);
        message.setOp(Message.PUTIFPRESENT);
        message.setExpire(TrueTime.currentTimeMillis() + unit.toMillis(ttl));

        Result<D> result = consensus.propose(message, true);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
    }

    @Override
    public <D extends Serializable> D get(final String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.GET);
        Result<D> result = consensus.read(message);
        if (!Result.State.SUCCESS.equals(result.getState())) {
            throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm");
        }
        return result.getData();
    }

    @Override
    public void invalidate(final String key) {
        Message message = new Message();
        message.setKey(key);
        message.setOp(Message.INVALIDATE);
        Result result = consensus.propose(message);
    }

    @Override
    public void invalidateAll() {
        Message message = new Message();
        message.setOp(Message.INVALIDATEALL);
        Result result = consensus.propose(message);
    }

}
