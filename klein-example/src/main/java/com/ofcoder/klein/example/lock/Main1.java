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

package com.ofcoder.klein.example.lock;

import com.ofcoder.klein.Klein;
import com.ofcoder.klein.KleinFactory;
import com.ofcoder.klein.KleinProp;
import com.ofcoder.klein.common.util.ThreadExecutor;
import com.ofcoder.klein.core.lock.KleinLock;
import com.ofcoder.klein.core.lock.LockSMSource;
import com.ofcoder.klein.rpc.facade.Endpoint;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main: operate cache.
 *
 * @author 释慧利
 */
public class Main1 {
    private static final Logger LOG = LoggerFactory.getLogger(Main1.class);

    public static void main(final String[] args) throws Exception {
        System.setProperty("klein.members", "1:127.0.0.1:1218:false;2:127.0.0.1:1219:false;3:127.0.0.1:1220:false");
        KleinProp prop1 = KleinProp.loadIfPresent();

        prop1.getConsensusProp().setSelf(new Endpoint("1", "127.0.0.1", 1218, false));
        prop1.getRpcProp().setPort(1218);

        Klein instance1 = Klein.startup();

        instance1.awaitInit();

        KleinLock klein = KleinFactory.getInstance().createKleinObject("klein", KleinLock.class, new LockSMSource());

        long start = System.currentTimeMillis();
        LOG.info("++++++++++first acquire: " + klein.acquire(1, TimeUnit.SECONDS) + ", cost: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        LOG.info("++++++++++second acquire: " + klein.acquire(1, TimeUnit.SECONDS) + ", cost: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        Thread.sleep(1000L);
        LOG.info("++++++++++third acquire: " + klein.acquire(100, TimeUnit.MILLISECONDS) + ", cost: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        klein.release();
        LOG.info("++++++++++third release, cost: " + (System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        LOG.info("++++++++++last acquire: " + klein.acquire(10, TimeUnit.MILLISECONDS) + ", cost: " + (System.currentTimeMillis() - start));

        AtomicInteger succ = new AtomicInteger();
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            ThreadExecutor.execute(() -> {
                try {
                    boolean acquire = klein.acquire(1, TimeUnit.SECONDS);
                    if (acquire) {
                        succ.getAndIncrement();
                    }
                    LOG.info("{}, succ: {}", finalI, acquire);
                } catch (Exception e) {
                    LOG.info("{}, err. ", finalI);
                }
            });
        }
        Thread.sleep(1000L);
        LOG.info("++++++++++succ: " + succ);

        System.in.read();
    }
}
