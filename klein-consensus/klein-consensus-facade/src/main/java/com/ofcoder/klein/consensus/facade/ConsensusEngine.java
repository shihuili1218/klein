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
package com.ofcoder.klein.consensus.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.consensus.facade.config.ConsensusProp;
import com.ofcoder.klein.spi.ExtensionLoader;

/**
 * Consensus Engine.
 *
 * @author far.liu
 */
public final class ConsensusEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ConsensusEngine.class);
    private static Consensus consensus;

    /**
     * engine start up.
     *
     * @param algorithm consensus type
     * @param prop      property
     */
    public static void startup(final String algorithm, final ConsensusProp prop) {
        LOG.info("start consensus engine");
        consensus = ExtensionLoader.getExtensionLoader(Consensus.class).register(algorithm, prop);
        consensus.preheating();
    }

    /**
     * shutdown.
     */
    public static void shutdown() {
        if (consensus != null) {
            consensus.shutdown();
        }
    }

}
