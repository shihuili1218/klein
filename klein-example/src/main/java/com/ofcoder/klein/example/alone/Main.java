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
package com.ofcoder.klein.example.alone;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.Klein;
import com.ofcoder.klein.KleinProp;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.core.cache.KleinCacheImpl;

/**
 * single node.
 *
 * @author 释慧利
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {
        KleinProp prop = KleinProp.loadIfPresent();

        Klein instance = Klein.startup();
        CountDownLatch latch = new CountDownLatch(1);
        instance.setMasterListener(master -> {
            LOG.info("master: " + master);
            if (master.getElectState() != null && master.getElectState().allowPropose()) {
                latch.countDown();
            }
        });
        latch.await();

        KleinCache klein1 = new KleinCacheImpl("klein1");
        KleinCache klein2 = new KleinCacheImpl("klein2");

        klein1.put("hello", "klein");
        LOG.info("======klein1 " + klein1.get("hello"));
        LOG.info("======klein2 " + klein2.get("hello"));

        klein1.put("hello", "test", 1L, TimeUnit.SECONDS);
        LOG.info(klein1.get("hello") + "========");
        Thread.sleep(500L);
        LOG.info(klein1.get("hello") + "========");
        Thread.sleep(500L);
        LOG.info(klein1.get("hello") + "========");
    }
}
