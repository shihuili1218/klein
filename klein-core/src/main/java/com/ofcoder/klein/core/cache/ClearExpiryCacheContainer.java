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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;

/**
 * @author 释慧利
 */
public abstract class ClearExpiryCacheContainer<D extends Serializable> implements CacheContainer<D> {

    private static final Map<Long, Set<String>> EXPIRY_BUCKETS = new ConcurrentHashMap<>();
    private final RepeatedTimer clearTask;
    public final int expirationInterval = 1000;

    public ClearExpiryCacheContainer() {
        clearTask = new RepeatedTimer("clear-expiry-cache", expirationInterval) {
            @Override
            protected void onTrigger() {

            }

            @Override
            protected int adjustTimeout(int timeoutMs) {
                return (int) (roundToNextInterval(TrueTime.currentTimeMillis()) - TrueTime.currentTimeMillis() + 10);
            }
        };
        clearTask.start();
    }

    @Override
    public D put(String key, D data, Long expire) {
        return null;
    }

    @Override
    public D putIfAbsent(String key, D data, Long expire) {
        return null;
    }

    private long roundToNextInterval(long time) {
        return (time / expirationInterval + 1) * expirationInterval;
    }

    /**
     * check whether the data has expired.
     *
     * @param key      value of key
     * @param metaData check data
     * @return whether expired
     */
    protected boolean checkExpire(final String key, final MetaData<D> metaData) {
        if (metaData == null) {
            return false;
        }
        if (metaData.getExpire() == Message.TTL_PERPETUITY) {
            return true;
        }
        if (metaData.getExpire() < TrueTime.currentTimeMillis()) {
            remove(key);
            return false;
        } else {
            return true;
        }
    }

}
