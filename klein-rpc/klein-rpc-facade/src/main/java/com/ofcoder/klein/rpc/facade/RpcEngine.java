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
package com.ofcoder.klein.rpc.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.util.Requires;
import com.ofcoder.klein.rpc.facade.config.RpcProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * Rpc Engine.
 *
 * @author far.liu
 */
public final class RpcEngine {
    private static final Logger LOG = LoggerFactory.getLogger(RpcEngine.class);
    private static RpcServer server;
    private static RpcClient client;

    /**
     * start up.
     *
     * @param rpc  rpc type
     * @param prop rpc property
     */
    public static void startup(final String rpc, final RpcProp prop) {
        LOG.info("start rpc engine");
        Requires.requireTrue(prop.getPort() > 0 && prop.getPort() < 0xFFFF, "port out of range:" + prop.getPort());

        server = ExtensionLoader.getExtensionLoader(RpcServer.class).register(rpc, prop);
        client = ExtensionLoader.getExtensionLoader(RpcClient.class).register(rpc, prop);
    }

    /**
     * register request processor.
     *
     * @param processor request processor
     */
    public static void registerProcessor(final RpcProcessor processor) {
        server.registerProcessor(processor);
    }

    /**
     * shutdown.
     */
    public static void shutdown() {
        if (server != null) {
            server.shutdown();
        }
        if (client != null) {
            client.shutdown();
        }
    }
}
