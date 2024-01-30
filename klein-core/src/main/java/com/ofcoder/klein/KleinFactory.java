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
package com.ofcoder.klein;

import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.core.cache.CacheProp;
import com.ofcoder.klein.core.cache.CacheSM;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.core.cache.KleinCacheImpl;
import com.ofcoder.klein.core.lock.KleinLock;
import com.ofcoder.klein.core.lock.KleinLockImpl;
import com.ofcoder.klein.core.lock.LockSM;

public final class KleinFactory {

    private KleinFactory() {
    }

    public static KleinFactory getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Return a new cache container.
     *
     * @param cacheName cacheName
     * @return cache container
     */
    public KleinCache createCache(final String cacheName) {

        String group = CacheSM.GROUP + "_" + cacheName;
        SMRegistry.register(group, new CacheSM(CacheProp.loadIfPresent()));

        return new KleinCacheImpl(group);
    }

    /**
     * Return a new lock instance.
     *
     * @param key lock name
     * @return lock instance
     */
    public KleinLock createLock(final String key) {

        String group = LockSM.GROUP + "_" + key;
        SMRegistry.register(group, new LockSM());

        return new KleinLockImpl(group);
    }

    private static class Holder {
        private static final KleinFactory INSTANCE = new KleinFactory();
    }
}
