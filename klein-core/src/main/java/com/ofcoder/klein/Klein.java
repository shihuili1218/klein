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

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.consensus.facade.Consensus;
import com.ofcoder.klein.consensus.facade.ConsensusEngine;
import com.ofcoder.klein.consensus.facade.MemberConfiguration;
import com.ofcoder.klein.consensus.paxos.core.MasterState;
import com.ofcoder.klein.consensus.paxos.core.RuntimeAccessor;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.spi.ExtensionLoader;
import com.ofcoder.klein.storage.facade.StorageEngine;

/**
 * Klein starter.
 *
 * @author far.liu
 */
public final class Klein {
    private static final Logger LOG = LoggerFactory.getLogger(Klein.class);
    private static volatile AtomicBoolean started = new AtomicBoolean(false);
    private ShutdownHook shutdownHook;

    private Klein() {
    }

    public void setMasterListener(MasterListener listener) {
        RuntimeAccessor.getMaster().addListener(listener::onChange);
    }

    public void setShutdownHook(final ShutdownHook shutdownHook) {
        this.shutdownHook = shutdownHook;
    }

    /**
     * startup klein.
     *
     * @return klein instance
     */
    public static Klein startup() {
        if (started.get()) {
            throw new StartupException("klein engine has started.");
        }
        Klein kl = KleinHolder.INSTANCE;

        if (!started.compareAndSet(false, true)) {
            LOG.warn("klein engine is starting.");
            return kl;
        }
        LOG.info("starting klein...");
        KleinProp prop = KleinProp.loadIfPresent();

        RpcEngine.startup(prop.getRpc(), prop.getRpcProp());
        StorageEngine.startup(prop.getStorage(), prop.getStorageProp());
        ConsensusEngine.startup(prop.getConsensus(), prop.getConsensusProp());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("*** shutting down Klein since JVM is shutting down");
            ConsensusEngine.shutdown();
            StorageEngine.shutdown();
            RpcEngine.shutdown();
            if (kl.shutdownHook != null) {
                kl.shutdownHook.tearingDown();
            }
            LOG.info("*** Klein shut down");
        }));
        return kl;
    }

    public MemberConfiguration getClusterInfo() {
        Consensus consensus = ExtensionLoader.getExtensionLoader(Consensus.class).getJoin();
        return consensus.getMemberConfig();
    }

    private static class KleinHolder {
        private static final Klein INSTANCE = new Klein();
    }

    public interface ShutdownHook {
        /**
         * tearingDown.
         */
        void tearingDown();
    }

    public interface MasterListener {
        void onChange(MasterState master);
    }
}
