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
package com.ofcoder.klein.example.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.Klein;
import com.ofcoder.klein.KleinProp;
import com.ofcoder.klein.rpc.facade.Endpoint;

/**
 * Main: operate cache.
 *
 * @author 释慧利
 */
public class Main1 {
    private static final Logger LOG = LoggerFactory.getLogger(Main1.class);

    public static void main(final String[] args) throws Exception {
        System.setProperty("klein.members", "1:127.0.0.1:1218;2:127.0.0.1:1219;3:127.0.0.1:1220");
        KleinProp prop1 = KleinProp.loadIfPresent();

        prop1.getConsensusProp().setSelf(new Endpoint("1", "127.0.0.1", 1218));
        prop1.getRpcProp().setPort(1218);

        Klein instance1 = Klein.startup();

        instance1.awaitInit();

//        Thread.sleep(15000L);

        String key = "hello";
        String value = "klein";
        long start = System.currentTimeMillis();
        LOG.info("++++++++++first put: " + instance1.getCache().put("hello1", "klein1") + ", cost: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        LOG.info("++++++++++second put: " + instance1.getCache().put("hello2", "klein2") + ", cost: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        LOG.info("++++++++++third put: " + instance1.getCache().put("hello3", "klein3") + ", cots: " + (System.currentTimeMillis() - start));

        LOG.info("----------get hello3: " + instance1.getCache().get("hello3"));
        LOG.info("----------get hello4: " + instance1.getCache().get("hello4"));
        LOG.info("----------exist hello3: " + instance1.getCache().exist("hello3"));
        LOG.info("----------exist hello4: " + instance1.getCache().exist("hello4"));
        LOG.info("----------putIfPresent hello4: " + instance1.getCache().putIfPresent("hello4", "klein4"));
        LOG.info("----------putIfPresent hello4: " + instance1.getCache().putIfPresent("hello4", "klein4.1"));
        instance1.getCache().invalidate("hello3");
        LOG.info("----------invalidate hello3");
        LOG.info("----------get hello3: " + instance1.getCache().get("hello3"));

        System.in.read();
    }
}
