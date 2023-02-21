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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import com.ofcoder.klein.common.util.TrueTime;
import com.ofcoder.klein.common.util.timer.RepeatedTimer;

/**
 * clear expire cache.
 *
 * @author 释慧利
 */
public abstract class ClearExpiryCacheContainer implements CacheContainer {

    private final Map<String, Set<String>> expiryBuckets = new ConcurrentHashMap<>();
    private final RepeatedTimer clearTask;
    private final int expirationInterval = 1000;

    public ClearExpiryCacheContainer() {
        clearTask = new RepeatedTimer("clear-expiry-cache", expirationInterval) {
            private static final String BUCKET_KEY = "clear";
            private static final int TASK_OFFSET = 5;

            @Override
            protected void onTrigger() {
                String bucket = ThreadContext.get(BUCKET_KEY);
                if (StringUtils.isEmpty(bucket) || !expiryBuckets.containsKey(bucket)) {
                    return;
                }
                Set<String> removed = expiryBuckets.remove(bucket);
                removed.forEach(i -> get(i));
            }

            @Override
            protected int adjustTimeout(final int timeoutMs) {
                long now = TrueTime.currentTimeMillis();
                long bucket = roundToNextBucket(now);
                ThreadContext.put(BUCKET_KEY, String.valueOf(bucket));
                return (int) (bucket - now + TASK_OFFSET);
            }
        };
        clearTask.start();
    }

    @Override
    public Object put(final String key, final Object data, final Long expire) {
        Object d = _put(key, data, expire);
        waitClear(expire, key);
        return d;
    }

    protected abstract Object _put(String key, Object data, Long expire);

    @Override
    public Object putIfAbsent(final String key, final Object data, final Long expire) {
        Object d = _putIfAbsent(key, data, expire);
        if (d == null) {
            waitClear(expire, key);
        }
        return d;
    }

    protected abstract Object _putIfAbsent(String key, Object data, Long expire);

    private long roundToNextBucket(final long time) {
        return (time / expirationInterval + 1) * expirationInterval;
    }

    private void waitClear(final Long expire, final String cache) {
        if (expire == Message.TTL_PERPETUITY) {
            return;
        }
        String bucket = String.valueOf(roundToNextBucket(expire));
        if (!expiryBuckets.containsKey(bucket)) {
            synchronized (expiryBuckets) {
                if (!expiryBuckets.containsKey(bucket)) {
                    expiryBuckets.put(bucket, new CopyOnWriteArraySet<>());
                }
            }
        }
        expiryBuckets.get(bucket).add(cache);
    }

    @Override
    public CacheSnap makeImage() {
        return new CacheSnap(_makeImage(), expiryBuckets);
    }

    protected abstract Map<String, MetaData> _makeImage();

    @Override
    @SuppressWarnings("unchecked")
    public void loadImage(final CacheSnap image) {
        expiryBuckets.clear();
        expiryBuckets.putAll(image.getExpiryBuckets());
        _loadImage((Map<String, MetaData>) image.getCache());
    }

    protected abstract void _loadImage(Map<String, MetaData> snap);

    /**
     * check whether the data has expired.
     *
     * @param key      value of key
     * @param metaData check data
     * @return whether expired
     */
    protected boolean checkExpire(final String key, final MetaData metaData) {
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

    @Override
    public void close() {
        clearTask.stop();
    }
}
