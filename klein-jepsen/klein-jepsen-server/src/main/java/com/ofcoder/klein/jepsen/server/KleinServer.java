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
package com.ofcoder.klein.jepsen.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofcoder.klein.Klein;
import com.ofcoder.klein.KleinFactory;
import com.ofcoder.klein.consensus.paxos.core.MasterState;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.KleinProp;
import com.ofcoder.klein.jepsen.server.rpc.ExistsProcessor;
import com.ofcoder.klein.jepsen.server.rpc.GetProcessor;
import com.ofcoder.klein.jepsen.server.rpc.InvalidateProcessor;
import com.ofcoder.klein.jepsen.server.rpc.PutProcessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * KleinServer for deploy klein.
 *
 * @author 释慧利
 */
public class KleinServer {
    static final Logger LOG = LoggerFactory.getLogger(KleinServer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    public static void main(final String[] args) throws Exception {
        KleinProp kleinProp = KleinProp.loadFromFile("config/server.properties");
        LOG.info(OBJECT_MAPPER.writeValueAsString(kleinProp));

        final Klein instance = Klein.startup();
        final CountDownLatch latch1 = new CountDownLatch(1);
        instance.setMasterListener(new Klein.MasterListener() {
            @Override
            public void onChange(MasterState master) {
                if (master.getElectState().allowPropose()){
                    latch1.countDown();
                }
            }
        });
        latch1.await();

        KleinCache cache = KleinFactory.getInstance().createCache("klein");

        RpcEngine.registerProcessor(new ExistsProcessor(cache));
        RpcEngine.registerProcessor(new GetProcessor(cache));
        RpcEngine.registerProcessor(new InvalidateProcessor(cache));
        RpcEngine.registerProcessor(new PutProcessor(cache));

        final CountDownLatch latch2 = new CountDownLatch(1);
        instance.setMasterListener(new Klein.MasterListener() {
            @Override
            public void onChange(MasterState master) {
                if (master.getElectState().allowPropose()){
                    latch2.countDown();
                }
            }
        });
        latch2.await();

        final CountDownLatch latch = new CountDownLatch(1);
        instance.setShutdownHook(new Klein.ShutdownHook() {
            @Override
            public void tearingDown() {
                latch.countDown();
            }
        });

        latch.await();
    }
}
