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

import com.ofcoder.klein.common.exception.KleinException;
import com.ofcoder.klein.consensus.facade.Result;
import com.ofcoder.klein.consensus.facade.sm.DefaultSM;
import com.ofcoder.klein.consensus.facade.sm.SMRegistry;
import com.ofcoder.klein.core.GroupWrapper;
import com.ofcoder.klein.core.cache.CacheProp;
import com.ofcoder.klein.core.cache.CacheSMSource;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.core.lock.KleinLock;
import com.ofcoder.klein.core.lock.LockSMSource;
import java.lang.reflect.Proxy;
import java.util.Arrays;

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
        try {
            return createKleinObject(cacheName, KleinCache.class, new CacheSMSource(CacheProp.loadIfPresent()));
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a new dynamic proxy  instance.
     *
     * @param key         操作 key
     * @param callerClazz 要代理的对象, 可以是interface
     * @param smInstance  具体实现对象
     * @param <I>         要代理的对象, 可以是interface
     * @param <T>         具体实现类, 可以序列化的
     * @return 操作结果
     * @throws InstantiationException 反射异常
     * @throws IllegalAccessException 反射异常
     */
    public <I, T extends I> I createKleinObject(final String key, final Class<I> callerClazz, final T smInstance) throws InstantiationException, IllegalAccessException {
        T smSource = smInstance;
        DefaultSM<T> sm = new DefaultSM<>(key, smSource);
        String group = sm.getGroup();

        SMRegistry.register(group, sm);

        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] {callerClazz}, (proxy, method, args) -> {
            GroupWrapper consensus = new GroupWrapper(group);

            int oldCapacity = args.length;
            int newCapacity = oldCapacity + 2;
            Object[] message = Arrays.copyOf(args, newCapacity);
            // key
            message[newCapacity - 1] = key;
            // op
            message[newCapacity - 2] = method.getName();

            Result result = consensus.propose(message, true);

            if (!Result.State.SUCCESS.equals(result.getState())) {
                throw new KleinException("The consensus negotiation result is UNKNOWN. In this case, the operation may or may not be completed. You need to retry or query to confirm. key: " + key);
            }

            return result.getData();
        });
    }

    /**
     * Return a new lock instance.
     *
     * @param key lock name
     * @return lock instance
     */
    public KleinLock createLock(final String key) {
        try {
            return KleinFactory.getInstance().createKleinObject(key, KleinLock.class, new LockSMSource());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Holder {
        private static final KleinFactory INSTANCE = new KleinFactory();
    }
}
