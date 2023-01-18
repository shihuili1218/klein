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
package com.ofcoder.klein.jepsen;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ofcoder.klein.Klein;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.jepsen.rpc.ExistsProcessor;
import com.ofcoder.klein.jepsen.rpc.GetProcessor;
import com.ofcoder.klein.jepsen.rpc.InvalidateProcessor;
import com.ofcoder.klein.jepsen.rpc.PutProcessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main: operate cache.
 *
 * @author 释慧利
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    public static void main(final String[] args) throws Exception {
        KleinProp kleinProp = KleinProp.loadFromFile("config/server.properties");
        LOG.info(OBJECT_MAPPER.writeValueAsString(kleinProp));

        Klein instance = Klein.startup();
        KleinCache cache = instance.getCache();
        RpcEngine.registerProcessor(new ExistsProcessor(cache));
        RpcEngine.registerProcessor(new GetProcessor(cache));
        RpcEngine.registerProcessor(new InvalidateProcessor(cache));
        RpcEngine.registerProcessor(new PutProcessor(cache));

        instance.awaitInit();
        System.in.read();
    }
}
