package com.ofcoder.klein;

import com.ofcoder.klein.common.exception.StartupException;
import com.ofcoder.klein.consensus.facade.ConsensusEngine;
import com.ofcoder.klein.core.cache.KleinCache;
import com.ofcoder.klein.core.cache.KleinCacheImpl;
import com.ofcoder.klein.core.config.KleinProp;
import com.ofcoder.klein.core.lock.KleinLock;
import com.ofcoder.klein.core.lock.KleinLockImpl;
import com.ofcoder.klein.rpc.facade.RpcEngine;
import com.ofcoder.klein.storage.facade.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author far.liu
 */
public class Klein {
    private static final Logger LOG = LoggerFactory.getLogger(Klein.class);
    private static volatile AtomicBoolean started = new AtomicBoolean(false);
    private KleinCache cache;
    private KleinLock lock;

    private void startup() {
        if (started.get()) {
            throw new StartupException("klein engine has started.");
        }
        if (!started.compareAndSet(false, true)) {
            LOG.warn("klein engine is starting.");
            return;
        }
        LOG.debug("starting klein...");
        KleinProp prop = KleinProp.loadIfPresent();

        ConsensusEngine.startup(prop.getConsensus(), prop.getConsensusProp());
        RpcEngine.startup(prop.getRpc(), prop.getRpcProp());
        StorageEngine.startup(prop.getStorage(), prop.getStorageProp());

        this.cache = new KleinCacheImpl();
        this.lock = new KleinLockImpl();
    }

    public KleinCache getCache() {
        return cache;
    }

    public KleinLock getLock() {
        return lock;
    }

    private Klein() {
        startup();
    }

    public static Klein getInstance() {
        return KleinHolder.INSTANCE;
    }

    private static class KleinHolder {
        private static final Klein INSTANCE = new Klein();
    }

}
